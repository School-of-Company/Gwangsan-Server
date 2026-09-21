package team.startup.gwangsan.domain.post.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.*;
import team.startup.gwangsan.domain.post.service.*;
import team.startup.gwangsan.domain.post.service.impl.FindProductByCurrentUserAndTypeAndModeServiceImpl;
import team.startup.gwangsan.domain.post.service.impl.FindProductsByMemberIdServiceImpl;
import team.startup.gwangsan.global.auth.MemberDetailsService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;
import team.startup.gwangsan.global.redis.RedisUtil;
import team.startup.gwangsan.global.security.config.SecurityConfig;
import team.startup.gwangsan.global.security.handler.JwtAccessDeniedHandler;
import team.startup.gwangsan.global.security.handler.JwtAuthenticationEntryPoint;
import team.startup.gwangsan.global.security.jwt.*;
import team.startup.gwangsan.global.util.MemberUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = SalesListIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.jpa.hibernate.ddl-auto=create", "spring.jpa.show-sql=false",
                "spring.jpa.open-in-view=false", "spring.flyway.enabled=false",
                "spring.docker.compose.enabled=false", "spring.jpa.properties.hibernate.generate_statistics=true",
                "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
                "jwt.access-secret=issue405-local-only-secret-at-least-32-bytes",
                "jwt.refresh-secret=issue405-local-only-refresh-secret-at-least-32-bytes"})
@DisplayName("판매 목록: 실제 HTTP/JWT 및 격리 MariaDB")
class SalesListIntegrationTest {
    @Container
    static final MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }

    @Configuration
    @EnableAutoConfiguration
    @EntityScan("team.startup.gwangsan.domain")
    @EnableJpaAuditing
    @EnableJpaRepositories(basePackages = {"team.startup.gwangsan.domain.post.repository",
            "team.startup.gwangsan.domain.member.repository"})
    @EnableConfigurationProperties(JwtProperties.class)
    @Import({PostController.class, FindProductByCurrentUserAndTypeAndModeServiceImpl.class,
            FindProductsByMemberIdServiceImpl.class, MemberUtil.class, QueryDslConfig.class,
            SecurityConfig.class, JwtProvider.class, TokenParser.class, MemberDetailsService.class,
            JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
    static class Config { }

    // 읽기 경로 외 서비스 및 외부 Redis만 대체한다. 조회/인증/SQL/직렬화는 실제 구현이다.
    @MockitoBean RedisUtil redisUtil;
    @MockitoBean CreateProductService createProductService;
    @MockitoBean ProductContentValidationService productContentValidationService;
    @MockitoBean FindProductByIdService findProductByIdService;
    @MockitoBean FindProductsByTypeAndModeService findProductsByTypeAndModeService;
    @MockitoBean UpdateProductService updateProductService;
    @MockitoBean DeleteProductByIdService deleteProductByIdService;
    @MockitoBean RequestTradeCompleteService requestTradeCompleteService;
    @MockitoBean WithdrawTradeCompleteService withdrawTradeCompleteService;
    @MockitoBean ReservationProductService reservationProductService;
    @MockitoBean DeleteReservationProductService deleteReservationProductService;

    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager transactions;
    @Autowired TestRestTemplate http;
    @Autowired JwtProvider jwt;
    @Autowired ObjectMapper json;
    @LocalServerPort int port;
    private static final AtomicInteger sequence = new AtomicInteger();

    @Nested
    @DisplayName("계약과 페이지 경계")
    class Contract {
        @Test
        void it_pages_both_routes_without_duplicates_and_preserves_legacy_arrays() throws Exception {
            Member owner = member();
            Member other = member();
            List<Long> expected = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                expected.add(product(owner, Type.OBJECT, Mode.GIVER,
                        i == 2 ? ProductStatus.RESERVATION : ProductStatus.ONGOING, i % 3));
            }
            product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.DELETED, 1);
            product(other, Type.OBJECT, Mode.GIVER, ProductStatus.ONGOING, 1);
            expected.sort(java.util.Comparator.reverseOrder());
            for (String path : paths(owner)) {
                assertThat(body(get(path, owner)).size()).isEqualTo(5);
                List<Long> collected = new ArrayList<>();
                String cursor = "";
                while (true) {
                    JsonNode page = body(get(path + "?size=2" + cursor, owner));
                    assertThat(page.size()).isLessThanOrEqualTo(2);
                    if (page.isEmpty()) break;
                    page.forEach(row -> {
                        collected.add(row.get("id").asLong());
                        assertThat(row.get("member").get("memberId").asLong()).isEqualTo(owner.getId());
                    });
                    cursor = "&last_id=" + page.get(page.size() - 1).get("id").asLong();
                    assertThat(collected.size()).isLessThanOrEqualTo(5);
                }
                assertThat(collected).containsExactlyElementsOf(expected);
                assertThat(body(get(path + "?size=1&last_id=1", owner))).isEmpty();
                assertThat(body(get(path + "?size=100", owner)).size()).isEqualTo(5);
            }
            assertThat(body(get("/api/post/member/" + other.getId() + "?size=2", owner)).size()).isEqualTo(1);
            assertThat(body(get("/api/post/member/" + member().getId() + "?size=2", owner))).isEmpty();
        }

        @Test
        void it_filters_before_limiting_and_includes_reservations_in_selling() throws Exception {
            Member owner = member();
            long ongoing = product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.ONGOING, 0);
            long reserved = product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.RESERVATION, 2);
            long completed = product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.COMPLETED, 1);
            product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.DELETED, 1);
            product(owner, Type.SERVICE, Mode.GIVER, ProductStatus.ONGOING, 1);
            product(owner, Type.OBJECT, Mode.RECEIVER, ProductStatus.ONGOING, 1);
            for (String path : paths(owner)) {
                String filter = "?size=1&type=OBJECT&mode=GIVER&completed=false";
                JsonNode first = body(get(path + filter, owner));
                assertThat(first.get(0).get("id").asLong()).isEqualTo(reserved);
                assertThat(first.get(0).get("isReserved").asBoolean()).isTrue();
                assertThat(first.get(0).get("isCompleted").asBoolean()).isFalse();
                assertThat(first.get(0).get("images").size()).isEqualTo(2);
                JsonNode next = body(get(path + filter + "&last_id=" + reserved, owner));
                assertThat(next.get(0).get("id").asLong()).isEqualTo(ongoing);
                assertThat(next.get(0).get("images")).isEmpty();
                JsonNode done = body(get(path + "?size=2&type=OBJECT&mode=GIVER&completed=true", owner));
                assertThat(done.size()).isEqualTo(1);
                assertThat(done.get(0).get("id").asLong()).isEqualTo(completed);
                assertThat(done.get(0).get("isCompleted").asBoolean()).isTrue();
                assertThat(done.get(0).get("isReserved").asBoolean()).isFalse();
                assertThat(body(get(path + "?size=10&type=SERVICE", owner)).size()).isEqualTo(1);
                assertThat(body(get(path + "?size=10&mode=RECEIVER", owner)).size()).isEqualTo(1);
            }
        }

        @Test
        void it_accepts_a_deleted_cursor_and_ignores_newer_inserts() throws Exception {
            Member owner = member();
            long oldest = product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.ONGOING, 0);
            long middle = product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.RESERVATION, 0);
            long cursor = product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.ONGOING, 0);
            for (String path : paths(owner)) {
                assertThat(body(get(path + "?size=1", owner)).get(0).get("id").asLong()).isEqualTo(cursor);
            }
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                em.find(Product.class, cursor).updateStatus(ProductStatus.DELETED);
                em.find(Product.class, middle).updateStatus(ProductStatus.COMPLETED);
            });
            product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.ONGOING, 0);
            for (String path : paths(owner)) {
                JsonNode all = body(get(path + "?size=10&last_id=" + cursor, owner));
                assertThat(all.size()).isEqualTo(2);
                assertThat(all.get(0).get("id").asLong()).isEqualTo(middle);
                JsonNode selling = body(get(path + "?size=10&completed=false&last_id=" + cursor, owner));
                assertThat(selling.size()).isEqualTo(1);
                assertThat(selling.get(0).get("id").asLong()).isEqualTo(oldest);
            }
        }

        @Test
        void it_rejects_bad_inputs_and_unauthenticated_requests() {
            Member owner = member();
            for (String path : paths(owner)) {
                for (String query : List.of("size=0", "size=-1", "size=101", "size=abc", "size=2147483648",
                        "size=2&last_id=0", "size=2&last_id=-1", "size=2&last_id=abc",
                        "size=2&last_id=9223372036854775808", "last_id=2", "completed=false",
                        "size=2&completed=invalid", "size=2&type=INVALID", "size=2&mode=INVALID")) {
                    assertThat(get(path + "?" + query, owner).getStatusCode().value()).as(query).isEqualTo(400);
                }
                assertThat(http.getForEntity(path + "?size=2", String.class).getStatusCode().value()).isEqualTo(401);
            }
            assertThat(get("/api/post/member/9223372036854775807?size=2", owner).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        void it_smokes_the_live_server_with_curl() throws Exception {
            Member owner = member();
            product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.RESERVATION, 1);
            for (String path : paths(owner)) {
                Process process = new ProcessBuilder("curl", "--silent", "--show-error", "--fail", "--max-time", "10",
                        "-H", "Authorization: Bearer " + token(owner),
                        "http://localhost:" + port + path + "?size=1&completed=false").start();
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertThat(process.waitFor()).isZero();
                assertThat(json.readTree(output).get(0).get("isReserved").asBoolean()).isTrue();
                System.out.printf("SALES_CURL path=%s status=200 rows=1 reserved=true%n", path);
            }
        }
    }

    @Nested
    @DisplayName("동일 데이터 성능 비교")
    class Measurements {
        @Test
        void it_measures_legacy_and_page_queries_bytes_and_latency() throws Exception {
            for (int count : List.of(10, 100, 1000)) {
                Member owner = member();
                new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    for (int i = 0; i < count; i++) persistProduct(owner, Type.OBJECT, Mode.GIVER, ProductStatus.ONGOING, 2);
                });
                for (String path : paths(owner)) {
                    for (String suffix : List.of("", "?size=20")) {
                        for (int warmup = 0; warmup < 3; warmup++) body(get(path + suffix, owner));
                        List<Double> times = new ArrayList<>();
                        long queries = 0;
                        int bytes = 0;
                        int rows = 0;
                        for (int sample = 0; sample < 7; sample++) {
                            var statistics = emf.unwrap(SessionFactory.class).getStatistics();
                            statistics.clear();
                            long start = System.nanoTime();
                            ResponseEntity<String> response = get(path + suffix, owner);
                            times.add((System.nanoTime() - start) / 1_000_000.0);
                            queries = statistics.getPrepareStatementCount();
                            rows = body(response).size();
                            bytes = response.getBody().getBytes(StandardCharsets.UTF_8).length;
                            assertThat(rows).isEqualTo(suffix.isEmpty() ? count : Math.min(count, 20));
                            // DTO enrichment cannot load products/images outside the requested page.
                            assertThat(statistics.getEntityStatistics(Product.class.getName()).getLoadCount()).isEqualTo(rows);
                            assertThat(statistics.getEntityStatistics(Image.class.getName()).getLoadCount()).isEqualTo(rows * 2L);
                            assertThat(queries).isEqualTo(7);
                        }
                        times.sort(Double::compareTo);
                        System.out.printf("SALES_BENCH count=%d path=%s mode=%s rows=%d queries=%d bytes=%d median_ms=%.3f%n",
                                count, path, suffix.isEmpty() ? "legacy" : "page20", rows, queries, bytes, times.get(3));
                    }
                }
                for (boolean paged : List.of(false, true)) {
                    String sql = "SELECT " + (paged ? "" : "DISTINCT ")
                            + "* FROM tbl_product WHERE member_id=" + owner.getId()
                            + " AND status IN ('ONGOING','RESERVATION','COMPLETED')"
                            + (paged ? " ORDER BY product_id DESC LIMIT 20" : "");
                    List<Double> sqlTimes = new ArrayList<>();
                    for (int sample = 0; sample < 10; sample++) {
                        String analysis = new TransactionTemplate(transactions).execute(status ->
                                (String) em.createNativeQuery("ANALYZE FORMAT=JSON " + sql).getSingleResult());
                        JsonNode plan = json.readTree(analysis).get("query_block");
                        if (sample >= 3) sqlTimes.add(plan.get("r_total_time_ms").asDouble());
                        if (sample == 9) System.out.printf("SALES_SQL_PLAN count=%d paged=%s plan=%s%n", count, paged, plan);
                    }
                    sqlTimes.sort(Double::compareTo);
                    System.out.printf("SALES_SQL count=%d paged=%s median_ms=%.4f%n", count, paged, sqlTimes.get(3));
                }

            }
        }
    }

    private Member member() {
        return new TransactionTemplate(transactions).execute(status -> {
            int id = sequence.incrementAndGet();
            Member member = Member.builder().name("test" + id).nickname("test" + id).phoneNumber("test" + id)
                    .password("unused").role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
            em.persist(member);
            Dong dong = Dong.builder().name("dong" + id).build();
            Place place = Place.builder().name("place" + id).build();
            em.persist(dong);
            em.persist(place);
            em.persist(MemberDetail.builder().member(member).dong(dong).place(place)
                    .gwangsan(10000).light(30).description("test").build());
            return member;
        });
    }

    private long product(Member owner, Type type, Mode mode, ProductStatus state, int images) {
        return new TransactionTemplate(transactions).execute(status -> persistProduct(owner, type, mode, state, images));
    }

    private long persistProduct(Member owner, Type type, Mode mode, ProductStatus state, int images) {
        Product product = Product.builder().member(em.getReference(Member.class, owner.getId())).title("판매 목록 테스트")
                .description("설명".repeat(100)).gwangsan(5000).type(type).mode(mode).status(state).build();
        em.persist(product);
        for (int i = 0; i < images; i++) {
            Image image = Image.builder().imageUrl("https://example.invalid/image/" + product.getId() + "/" + i).build();
            em.persist(image);
            em.persist(ProductImage.builder().product(product).image(image).build());
        }
        return product.getId();
    }

    private List<String> paths(Member owner) {
        return List.of("/api/post/current", "/api/post/member/" + owner.getId());
    }

    private String token(Member owner) {
        return jwt.generateAccessToken(owner.getPhoneNumber(), owner.getRole());
    }

    private ResponseEntity<String> get(String path, Member owner) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token(owner));
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode().value()).as(response.getBody()).isEqualTo(200);
        JsonNode body = json.readTree(response.getBody());
        assertThat(body.isArray()).isTrue();
        return body;
    }
}
