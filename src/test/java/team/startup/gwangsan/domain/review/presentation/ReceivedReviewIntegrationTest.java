package team.startup.gwangsan.domain.review.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.*;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.service.CreateReviewService;
import team.startup.gwangsan.domain.review.service.GetMyReviewListService;
import team.startup.gwangsan.domain.review.service.GetReviewDetailService;
import team.startup.gwangsan.domain.review.service.impl.GetReceivedReviewListServiceImpl;
import team.startup.gwangsan.domain.review.service.impl.GetReviewByMemberServiceImpl;
import team.startup.gwangsan.global.auth.MemberDetailsService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;
import team.startup.gwangsan.global.redis.RedisConfig;
import team.startup.gwangsan.global.redis.RedisUtil;
import team.startup.gwangsan.global.security.config.SecurityConfig;
import team.startup.gwangsan.global.security.handler.JwtAccessDeniedHandler;
import team.startup.gwangsan.global.security.handler.JwtAuthenticationEntryPoint;
import team.startup.gwangsan.global.security.jwt.*;
import team.startup.gwangsan.global.util.MemberUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ReceivedReviewIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.docker.compose.enabled=false", "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create", "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
        "logging.level.team.startup.gwangsan.global.filter.RequestLogFilter=OFF",
        "jwt.access-secret=issue406-local-test-key-01234567890123456789",
        "jwt.refresh-secret=issue406-local-refresh-01234567890123456789"})
@Testcontainers
@DisplayName("받은 후기 실제 HTTP/MariaDB 통합 검증")
class ReceivedReviewIntegrationTest {
    @Container static final MariaDBContainer<?> db = new MariaDBContainer<>("mariadb:11.4");
    @Container static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", db::getJdbcUrl);
        registry.add("spring.datasource.username", db::getUsername);
        registry.add("spring.datasource.password", db::getPassword);
        registry.add("spring.datasource.driver-class-name", db::getDriverClassName);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Configuration
    @EnableAutoConfiguration
    @EntityScan("team.startup.gwangsan.domain")
    @EnableJpaRepositories("team.startup.gwangsan.domain")
    @EnableJpaAuditing
    @EnableConfigurationProperties(JwtProperties.class)
    @Import({ReviewController.class, GetReceivedReviewListServiceImpl.class, GetReviewByMemberServiceImpl.class,
            QueryDslConfig.class, MemberUtil.class, SecurityConfig.class, JwtProvider.class, TokenParser.class,
            MemberDetailsService.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
            GlobalExceptionHandler.class, RedisConfig.class, RedisUtil.class})
    static class Config {
        @Bean
        HibernatePropertiesCustomizer captureSql() {
            return properties -> properties.put("hibernate.session_factory.statement_inspector", (StatementInspector) sql -> {
                if (sql.startsWith("select") && (sql.contains(" from tbl_review ") || sql.contains(" from tbl_product_image "))
                        && sql.chars().filter(ch -> ch == '?').count() <= 21) SQL.add(sql);
                return sql;
            });
        }
    }

    private static final Set<String> SQL = ConcurrentHashMap.newKeySet();

    @MockitoBean CreateReviewService create;
    @MockitoBean GetMyReviewListService written;
    @MockitoBean GetReviewDetailService detail;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JwtProvider jwt;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Nested
    @DisplayName("페이지 경계 및 기존 전체 계약")
    class Pagination {
        @Test
        void it_keeps_legacy_arrays_and_pages_scoped_with_images() throws Exception {
            Fixture fixture = fixture(5);
            for (String path : paths(fixture)) {
                JsonNode all = body(get(path, fixture.token()));
                assertThat(all.size()).isEqualTo(5);
                Statistics stats = statistics();
                stats.clear();
                JsonNode first = body(get(path + "?size=2", fixture.token()));
                assertThat(first).isEqualTo(mapper.valueToTree(List.of(all.get(0), all.get(1))));
                assertThat(stats.getPrepareStatementCount()).isEqualTo(5);
                assertThat(stats.getEntityLoadCount()).isEqualTo(10); // 2 auth + target + 1 product + 3 image/link pairs
                assertThat(first.get(0).get("reviewerName").asText()).isEqualTo(fixture.reviewerName());
                assertThat(first.get(0).get("targetName").asText()).isEqualTo(fixture.targetName());
                assertThat(first.get(0).get("content").asText()).isEqualTo("review-4");
                assertThat(first.get(0).get("light").asInt()).isEqualTo(7);
                assertThat(first.get(0).get("imageUrls").size()).isEqualTo(3);
                assertThat(first.get(0).get("imageUrls").get(0).get("imageId").asLong()).isPositive();
                assertThat(first.get(0).get("imageUrls").get(0).get("imageUrl").asText()).startsWith("local/406/");
                assertThat(first.get(1).get("imageUrls").size()).isZero();

                long cursor = first.get(1).get("reviewId").asLong();
                JsonNode next = body(get(path + "?size=2&cursor=" + cursor, fixture.token()));
                assertThat(next).isEqualTo(mapper.valueToTree(List.of(all.get(2), all.get(3))));
                JsonNode last = body(get(path + "?size=2&cursor=" + next.get(1).get("reviewId"), fixture.token()));
                assertThat(last).isEqualTo(mapper.valueToTree(List.of(all.get(4))));
                stats.clear();
                assertThat(body(get(path + "?size=2&cursor=" + last.get(0).get("reviewId"), fixture.token())).size()).isZero();
                assertThat(stats.getPrepareStatementCount()).isEqualTo(4); // no image query
                assertThat(body(get(path + "?size=100&cursor=" + Long.MAX_VALUE, fixture.token()))).isEqualTo(all);
            }
        }

        @Test
        void it_keeps_requested_member_separate_from_authenticated_member() throws Exception {
            Fixture fixture = fixture(4);
            JsonNode current = body(get("/api/review/current?size=2&memberId=" + fixture.otherId(), fixture.token()));
            for (JsonNode row : current) assertThat(row.get("targetName").asText()).isEqualTo(fixture.targetName());
            JsonNode other = body(get("/api/review/" + fixture.otherId() + "?size=2", fixture.token()));
            for (JsonNode row : other) {
                assertThat(row.get("targetName").asText()).isEqualTo(fixture.reviewerName());
                assertThat(row.get("reviewerName").asText()).isEqualTo(fixture.targetName());
                assertThat(row.get("content").asText()).isEqualTo("other-member");
            }
            JsonNode boundary = body(get("/api/review/current?size=2&cursor=" + other.get(0).get("reviewId"), fixture.token()));
            assertThat(boundary).isEqualTo(current);
        }

        @Test
        void it_handles_exact_boundary_deleted_cursor_and_new_inserts() throws Exception {
            Fixture fixture = fixture(4);
            String path = "/api/review/" + fixture.targetId();
            JsonNode first = body(get(path + "?size=2", fixture.token()));
            long cursor = first.get(1).get("reviewId").asLong();
            jdbc.update("DELETE FROM tbl_review WHERE review_id=?", cursor);
            jdbc.update("INSERT INTO tbl_review(content,light,reviewer_id,reviewed_id,product_id) "
                            + "SELECT 'new',7,reviewer_id,reviewed_id,product_id FROM tbl_review WHERE review_id=?",
                    first.get(0).get("reviewId").asLong());
            JsonNode next = body(get(path + "?size=2&cursor=" + cursor, fixture.token()));
            assertThat(next.size()).isEqualTo(2);
            for (JsonNode row : next) assertThat(row.get("reviewId").asLong()).isLessThan(cursor);
            assertThat(body(get(path + "?size=2&cursor=" + next.get(1).get("reviewId"), fixture.token())).size()).isZero();
            assertThat(body(get(path + "?size=1", fixture.token())).get(0).get("content").asText()).isEqualTo("new");
        }

        @Test
        void it_returns_empty_for_no_reviews_and_404_for_missing_member() throws Exception {
            Fixture empty = fixture(0);
            for (String path : paths(empty)) {
                assertThat(body(get(path, empty.token())).size()).isZero();
                assertThat(body(get(path + "?size=2", empty.token())).size()).isZero();
            }
            assertThat(get("/api/review/" + Long.MAX_VALUE + "?size=2", empty.token()).statusCode()).isEqualTo(404);
        }

        @Test
        void it_uses_real_jwt_security_and_curl() throws Exception {
            Fixture fixture = fixture(3);
            for (String path : paths(fixture)) {
                assertThat(get(path + "?size=2", null).statusCode()).isEqualTo(401);
                assertThat(get(path + "?size=2", "invalid-token").statusCode()).isEqualTo(401);
                assertThat(get(path + "?size=0", fixture.token()).statusCode()).isEqualTo(400);
                assertThat(get(path + "?cursor=2", fixture.token()).statusCode()).isEqualTo(400);
                Process curl = new ProcessBuilder("curl", "--silent", "--show-error", "--fail-with-body",
                        "--max-time", "10", "-H", "Authorization: Bearer " + fixture.token(),
                        "http://127.0.0.1:" + port + path + "?size=2").start();
                JsonNode result = mapper.readTree(curl.getInputStream().readAllBytes());
                assertThat(curl.waitFor()).isZero();
                assertThat(result.size()).isEqualTo(2);
                System.out.println("CURL " + path + "?size=2 -> 200 rows=" + result.size());
            }
        }
    }

    @Test
    @DisplayName("동일 데이터 전체/페이지 HTTP 비용과 실제 인덱스 실행계획을 기록한다")
    void it_measures_bounded_work_on_identical_data() throws Exception {
        for (int count : new int[]{10, 100, 1000}) {
            Fixture fixture = fixture(count);
            jdbc.execute("ANALYZE TABLE tbl_review, tbl_member, tbl_product, tbl_product_image, tbl_image");
            for (String path : paths(fixture)) {
                for (int warmup = 0; warmup < 3; warmup++) {
                    get(path, fixture.token());
                    get(path + "?size=20", fixture.token());
                }
                List<Measurement> full = new ArrayList<>();
                List<Measurement> page = new ArrayList<>();
                for (int run = 0; run < 10; run++) {
                    full.add(measure(path, fixture.token(), count));
                    page.add(measure(path + "?size=20", fixture.token(), Math.min(count, 20)));
                }
                report(count, path, "full", full);
                report(count, path, "page20", page);
            }
            measureServerSql(fixture, count);
            if (count == 1000) {
                body(get("/api/review/current?size=20&cursor=" + fixture.middleId(), fixture.token()));
                SQL.forEach(sql -> System.out.println("ACTUAL_SQL " + sql));
                System.out.println("HQL " + java.util.Arrays.toString(statistics().getQueries()));
                System.out.println("INDEXES " + jdbc.queryForList("SHOW INDEX FROM tbl_review"));
                String sql = "SELECT r.review_id,r.product_id,r.content,r.light,m.nickname FROM tbl_review r "
                        + "JOIN tbl_member m ON m.member_id=r.reviewer_id WHERE r.reviewed_id=" + fixture.targetId();
                for (String suffix : List.of(" ORDER BY r.review_id DESC", " ORDER BY r.review_id DESC LIMIT 20",
                        " AND r.review_id < " + fixture.middleId() + " ORDER BY r.review_id DESC LIMIT 20")) {
                    System.out.println("EXPLAIN " + sql + suffix + " => " + jdbc.queryForList("EXPLAIN " + sql + suffix));
                    System.out.println("ANALYZE " + jdbc.queryForObject("ANALYZE FORMAT=JSON " + sql + suffix, String.class));
                }
            }
        }
    }

    private void measureServerSql(Fixture fixture, int count) throws Exception {
        String reviewSql = SQL.stream().filter(sql -> sql.contains(" from tbl_review ") && !sql.contains("limit"))
                .findFirst().orElseThrow().replace("?", Long.toString(fixture.targetId()));
        String imageSql = SQL.stream().filter(sql -> sql.contains(" from tbl_product_image ")).findFirst().orElseThrow();
        for (boolean page : new boolean[]{false, true}) {
            String limit = page ? " LIMIT 20" : "";
            List<Long> ids = jdbc.queryForList("SELECT product_id FROM tbl_review WHERE reviewed_id=? ORDER BY review_id DESC" + limit,
                    Long.class, fixture.targetId());
            String idList = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            String images = imageSql.substring(0, imageSql.indexOf(" where ")) + " where p1_0.product_id in (" + idList + ")";
            JsonNode reviewPlan = mapper.readTree(jdbc.queryForObject("ANALYZE FORMAT=JSON " + reviewSql + limit, String.class));
            JsonNode imagePlan = mapper.readTree(jdbc.queryForObject("ANALYZE FORMAT=JSON " + images, String.class));
            System.out.printf("SERVER_SQL n=%d mode=%s reviewMs=%s imagesMs=%s productIds=%d%n", count,
                    page ? "page20" : "full", reviewPlan.at("/query_block/r_total_time_ms"),
                    imagePlan.at("/query_block/r_total_time_ms"), ids.size());
        }
    }

    private Measurement measure(String path, String token, int count) throws Exception {
        Statistics stats = statistics();
        stats.clear();
        long start = System.nanoTime();
        var response = get(path, token);
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        assertThat(body(response).size()).isEqualTo(count);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(5);
        long reviewMillis = 0;
        long imageMillis = 0;
        for (String query : stats.getQueries()) {
            if (query.contains("from Review ")) reviewMillis += stats.getQueryStatistics(query).getExecutionTotalTime();
            if (query.contains("from ProductImage ")) imageMillis += stats.getQueryStatistics(query).getExecutionTotalTime();
        }
        return new Measurement(millis, response.body().length, stats.getPrepareStatementCount(),
                stats.getEntityLoadCount(), reviewMillis, imageMillis);
    }

    private void report(int count, String path, String mode, List<Measurement> samples) {
        double[] times = samples.stream().mapToDouble(Measurement::millis).sorted().toArray();
        Measurement last = samples.getLast();
        System.out.printf("BENCH n=%d path=%s mode=%s rows=%d bytes=%d queries=%d entities=%d "
                        + "httpMedianMs=%.3f httpP95Ms=%.3f reviewQueryAvgMs=%.3f imageQueryAvgMs=%.3f%n",
                count, path, mode, mode.equals("full") ? count : Math.min(count, 20), last.bytes(),
                last.queries(), last.entities(), (times[4] + times[5]) / 2, times[9],
                samples.stream().mapToLong(Measurement::reviewMillis).average().orElseThrow(),
                samples.stream().mapToLong(Measurement::imageMillis).average().orElseThrow());
    }

    private Statistics statistics() { return emf.unwrap(SessionFactory.class).getStatistics(); }

    private HttpResponse<byte[]> get(String path, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(20));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private JsonNode body(HttpResponse<byte[]> response) throws Exception {
        assertThat(response.statusCode()).as(new String(response.body(), StandardCharsets.UTF_8)).isEqualTo(200);
        return mapper.readTree(response.body());
    }

    private List<String> paths(Fixture fixture) { return List.of("/api/review/current", "/api/review/" + fixture.targetId()); }

    private Fixture fixture(int count) {
        return new TransactionTemplate(transactions).execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Member target = member("target" + suffix);
            Member other = member("other" + suffix);
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Product product = persist(Product.builder().member(target).title("product" + i).description("description")
                        .gwangsan(5000).type(Type.OBJECT).mode(Mode.GIVER).status(ProductStatus.COMPLETED).build());
                if (i % 2 == 0) {
                    for (int image = 0; image < 3; image++) {
                        persist(ProductImage.builder().product(product)
                                .image(persist(Image.builder().imageUrl("local/406/" + i + "/" + image).build())).build());
                    }
                }
                ids.add(persist(Review.builder().reviewer(other).reviewed(target).product(product)
                        .content("review-" + i).light(7).build()).getId());
                persist(Review.builder().reviewer(target).reviewed(other).product(product)
                        .content("other-member").light(3).build());
            }
            em.flush();
            return new Fixture(target.getId(), other.getId(), target.getNickname(), other.getNickname(),
                    jwt.generateAccessToken(target.getPhoneNumber(), target.getRole()),
                    ids.isEmpty() ? 1L : ids.get(ids.size() / 2));
        });
    }

    private Member member(String name) {
        return persist(Member.builder().name(name).nickname(name).phoneNumber(name).password("local-test")
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build());
    }

    private <T> T persist(T entity) { em.persist(entity); return entity; }
    private record Fixture(long targetId, long otherId, String targetName, String reviewerName, String token, long middleId) {}
    private record Measurement(double millis, int bytes, long queries, long entities, long reviewMillis, long imageMillis) {}
}
