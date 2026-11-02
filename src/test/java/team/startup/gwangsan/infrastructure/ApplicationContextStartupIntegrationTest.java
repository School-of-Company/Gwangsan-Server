package team.startup.gwangsan.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.GwangsanApplication;
import team.startup.gwangsan.adapter.security.auth.ActorContextArgumentResolver;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * production component scan 으로 앱을 띄워 PR01 의 패키지 이동(infrastructure/adapter.security)과 wiring 을 확인한다.
 * create-drop 대신 create 를 써서 컨테이너 종료 후 스키마 drop 타임아웃(기준선 이슈)을 만들지 않는다.
 */
@SpringBootTest(classes = ApplicationContextStartupIntegrationTest.ProductionScan.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.docker.compose.enabled=false", "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create"})
@ActiveProfiles("test")
@Testcontainers
// 테스트는 기본으로 metric export(Prometheus registry)를 끈다. 실제 scrape 형식을 보려고 켠다.
@AutoConfigureObservability(tracing = false)
@DisplayName("전체 애플리케이션 기동과 인증 경계")
class ApplicationContextStartupIntegrationTest {

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

    /**
     * GwangsanApplication 과 같은 구성에서 test 출력 클래스만 scan 에서 뺀다.
     * 기존 일부 테스트의 nested @Configuration 이 TestTypeExcludeFilter 를 통과해 전체 scan 에 섞이기 때문이다(기준선 이슈).
     * ponytail: GwangsanApplication 의 annotation 이 바뀌면 여기도 맞춘다.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @AutoConfigurationPackage(basePackageClasses = GwangsanApplication.class)
    @ConfigurationPropertiesScan(basePackageClasses = GwangsanApplication.class)
    @EnableJpaAuditing
    @ComponentScan(basePackageClasses = GwangsanApplication.class, excludeFilters = {
            @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
            @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
            @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TestOutputExcludeFilter.class),
            // main 클래스를 scan 하면 그 @ComponentScan 이 필터 없이 다시 실행된다.
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GwangsanApplication.class)})
    static class ProductionScan {
    }

    static class TestOutputExcludeFilter implements TypeFilter {
        @Override
        public boolean match(MetadataReader reader, MetadataReaderFactory factory) throws IOException {
            return reader.getResource().getURL().getPath().contains("/classes/java/test/");
        }
    }

    @Autowired
    RequestMappingHandlerAdapter handlerAdapter;
    @LocalServerPort
    int port;
    @LocalManagementPort
    int managementPort;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Nested
    @DisplayName("기동하면")
    class Describe_startup {

        @Test
        @DisplayName("it_ActorContext resolver 를 MVC 인자 resolver 로 등록한다")
        void it_registers_actor_context_resolver() {
            assertThat(handlerAdapter.getArgumentResolvers())
                    .anyMatch(ActorContextArgumentResolver.class::isInstance);
        }
    }

    @Nested
    @DisplayName("보안 필터 체인은 기존 계약대로")
    class Describe_security_chain {

        @Test
        @DisplayName("it_공개 경로 health 는 토큰 없이 200 을 반환한다")
        void it_permits_health() throws Exception {
            assertThat(get("/api/health", null).statusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("it_보호 경로에 토큰이 없으면 401 을 반환한다")
        void it_rejects_missing_token() throws Exception {
            assertThat(get("/api/alert", null).statusCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("it_잘못된 JWT 는 ExceptionFilter 가 INVALID_TOKEN 401 JSON 으로 응답한다")
        void it_rejects_invalid_token() throws Exception {
            HttpResponse<String> response = get("/api/alert", "Bearer not-a-jwt");

            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("\"status\":401").contains("유효하지 않은 토큰입니다.");
        }
    }

    @Nested
    @DisplayName("Actuator 는 내부 management 포트에서만")
    class Describe_actuator {

        @Test
        @DisplayName("it_prometheus 로 JVM·Hikari·HTTP histogram·Tomcat·executor·stream snapshot metric 을 낸다")
        void it_exposes_prometheus_on_management_port() throws Exception {
            get("/api/health", null);

            HttpResponse<String> response = get(managementPort, "/actuator/prometheus", null);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .contains("jvm_memory_used_bytes")
                    .contains("hikaricp_connections_active")
                    .contains("http_server_requests_seconds_bucket")
                    .contains("le=\"0.005\"")
                    .contains("tomcat_threads_busy_threads")
                    .contains("executor_active_threads")
                    .contains("gwangsan_stream_snapshot_success{group=\"rooms\"}")
                    .contains("gwangsan_stream_snapshot_timestamp_seconds{group=\"retry\"}")
                    .contains("gwangsan_chat_publish_to_commit_seconds_bucket")
                    .doesNotContain("service=\"").doesNotContain("instance=\"");
        }

        @Test
        @DisplayName("it_health 는 상세 없이 열고 그 밖의 endpoint 는 노출하지 않는다")
        void it_limits_exposure() throws Exception {
            HttpResponse<String> health = get(managementPort, "/actuator/health", null);

            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(health.body()).doesNotContain("details").doesNotContain("components");
            assertThat(get(managementPort, "/actuator/env", null).statusCode()).isNotEqualTo(200);
        }

        @Test
        @DisplayName("it_API 포트에서는 metric 을 내지 않는다")
        void it_does_not_expose_metrics_on_api_port() throws Exception {
            HttpResponse<String> response = get("/actuator/prometheus", null);

            assertThat(response.statusCode()).isNotEqualTo(200);
            assertThat(response.body()).doesNotContain("jvm_memory_used_bytes");
            assertThat(managementPort).isNotEqualTo(port);
        }
    }

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        return get(port, path, authorization);
    }

    private HttpResponse<String> get(int targetPort, String path, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + targetPort + path)).GET();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
