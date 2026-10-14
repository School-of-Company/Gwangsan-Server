package team.startup.gwangsan.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.application.sms.port.out.SmsSenderPort;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.adapter.persistence.place.HeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.global.redis.RedisUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 MariaDB·Redis 와 production wiring 으로 가입 → 로그인 → 재발급 → 로그아웃 경계를 확인한다.
 * SMS 제공자 호출만 대체한다. 관리자 승인은 PR11 범위라 SQL 로 상태만 바꾼다.
 */
@SpringBootTest(classes = ApplicationContextStartupIntegrationTest.ProductionScan.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.docker.compose.enabled=false", "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create",
                "jwt.access-secret=pr02-boundary-local-only-access-secret-32-bytes",
                "jwt.refresh-secret=pr02-boundary-local-only-refresh-secret-32-bytes"})
@ActiveProfiles("test")
@Testcontainers
@DisplayName("회원·인증·SMS 경계 통합")
class MemberAuthBoundaryIntegrationTest {

    private static final String PHONE = "01055556666";

    @Container
    static final MariaDBContainer<?> db = new MariaDBContainer<>("mariadb:11.4");
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", db::getJdbcUrl);
        registry.add("spring.datasource.username", db::getUsername);
        registry.add("spring.datasource.password", db::getPassword);
        registry.add("spring.datasource.driver-class-name", db::getDriverClassName);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @MockitoBean SmsSenderPort smsSenderPort;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired RedisUtil redisUtil;
    @LocalServerPort int port;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    private Integer placeId;

    @BeforeEach
    void seed() {
        placeId = transactionTemplate.execute(status -> {
            HeadJpaEntity head = new HeadJpaEntity("광산구");
            entityManager.persist(head);
            DongJpaEntity dong = new DongJpaEntity("수완동");
            entityManager.persist(dong);
            PlaceJpaEntity place = new PlaceJpaEntity("수완지점", head);
            entityManager.persist(place);
            MemberJpaEntity recommender = MemberJpaEntity.builder().name("추천인").nickname("추천인").password("x")
                    .phoneNumber("01000000001").role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
            entityManager.persist(recommender);
            entityManager.persist(MemberDetailJpaEntity.builder().member(recommender)
                    .dong(dong).place(place)
                    .gwangsan(0).light(1).description("추천인").build());
            return place.getId();
        });
    }

    @Test
    @DisplayName("it_가입부터 로그아웃까지 기존 HTTP·DB·Redis 계약을 유지한다")
    void it_keeps_identity_flow_contract() throws Exception {
        // SMS 인증
        assertThat(send("POST", "/api/sms", "{\"phoneNumber\":\"" + PHONE + "\"}", null).statusCode()).isEqualTo(200);
        String code = redisUtil.get("sms:code:" + PHONE, String.class);
        assertThat(code).matches("\\d{6}");
        String wrongCode = code.equals("000001") ? "000002" : "000001";
        assertError(send("POST", "/api/sms/verify", "{\"phoneNumber\":\"" + PHONE + "\",\"code\":\"" + wrongCode + "\"}", null),
                400, "인증 번호가 일치하지 않습니다.");
        assertThat(send("POST", "/api/sms/verify", "{\"phoneNumber\":\"" + PHONE + "\",\"code\":\"" + code + "\"}", null)
                .statusCode()).isEqualTo(200);

        // 가입
        String signUp = "{\"name\":\"홍길동\",\"nickname\":\"길동\",\"password\":\"password\",\"phoneNumber\":\"" + PHONE
                + "\",\"dongName\":\"수완동\",\"placeId\":" + placeId
                + ",\"specialties\":[\"요리\"],\"recommender\":\"추천인\",\"description\":\"소개\"}";
        HttpResponse<String> created = send("POST", "/api/auth/signup", signUp, null);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).isEmpty();
        assertThat(jdbcTemplate.queryForMap("SELECT m.member_status, m.role, d.gwangsan, d.light FROM tbl_member m "
                + "JOIN tbl_member_detail d ON d.member_id = m.member_id WHERE m.phone_number = ?", PHONE))
                .containsEntry("member_status", "PENDING").containsEntry("role", "ROLE_USER")
                .containsEntry("gwangsan", 0).containsEntry("light", 1);
        assertThat(redisUtil.get("sms:verified:" + PHONE, Boolean.class)).isNull();
        assertThat(send("POST", "/api/auth/signup", signUp, null).statusCode()).isEqualTo(409);

        // 승인 전 로그인 거절, 승인 후 로그인
        String signIn = "{\"nickname\":\"길동\",\"password\":\"password\"}";
        assertError(send("POST", "/api/auth/signin", signIn, null), 403, "승인 대기 중인 계정입니다. 관리자 승인 후 이용 가능합니다.");
        jdbcTemplate.update("UPDATE tbl_member SET member_status = 'ACTIVE' WHERE phone_number = ?", PHONE);
        assertError(send("POST", "/api/auth/signin", "{\"nickname\":\"길동\",\"password\":\"wrong\"}", null),
                401, "닉네임 또는 비밀번호가 잘못되었습니다.");
        JsonNode tokens = json.readTree(send("POST", "/api/auth/signin", signIn, null).body());
        String access = tokens.path("accessToken").asText();
        String refresh = tokens.path("refreshToken").asText();

        JsonNode me = json.readTree(send("GET", "/api/auth", null, "Bearer " + access).body());
        assertThat(me.path("nickname").asText()).isEqualTo("길동");
        JsonNode myInfo = json.readTree(send("GET", "/api/member", null, "Bearer " + access).body());
        assertThat(myInfo.path("dongName").asText()).isEqualTo("광산구");
        assertThat(myInfo.path("headName").asText()).isEqualTo("수완동");
        assertThat(myInfo.path("specialties").get(0).asText()).isEqualTo("요리");

        // 재발급: 같은 초에 만든 JWT 는 같은 문자열이므로 1초 넘게 기다린다.
        Thread.sleep(1_100);
        HttpResponse<String> reissued = send("PATCH", "/api/auth/reissue", null, null, "RefreshToken", refresh);
        assertThat(reissued.statusCode()).isEqualTo(200);
        String newAccess = json.readTree(reissued.body()).path("accessToken").asText();
        assertThat(send("PATCH", "/api/auth/reissue", null, null, "RefreshToken", refresh).statusCode()).isEqualTo(401);

        // 로그아웃 후 같은 access token 거절
        assertThat(send("DELETE", "/api/auth/signout", null, "Bearer " + newAccess).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/api/auth", null, "Bearer " + newAccess).statusCode()).isEqualTo(401);
    }

    private void assertError(HttpResponse<String> response, int status, String message) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(json.readTree(response.body())).isEqualTo(json.readTree(
                "{\"status\":" + status + ",\"message\":\"" + message + "\"}"));
    }

    private HttpResponse<String> send(String method, String path, String body, String authorization) throws Exception {
        return send(method, path, body, authorization, null, null);
    }

    private HttpResponse<String> send(String method, String path, String body, String authorization,
                                      String header, String value) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (body != null) request.header("Content-Type", "application/json");
        if (authorization != null) request.header("Authorization", authorization);
        if (header != null) request.header(header, value);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
