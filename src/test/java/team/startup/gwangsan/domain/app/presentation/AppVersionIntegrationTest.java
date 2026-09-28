package team.startup.gwangsan.domain.app.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.domain.app.service.impl.FindAppVersionServiceImpl;
import team.startup.gwangsan.global.auth.MemberDetailsService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.global.security.config.SecurityConfig;
import team.startup.gwangsan.global.security.handler.JwtAccessDeniedHandler;
import team.startup.gwangsan.global.security.handler.JwtAuthenticationEntryPoint;
import team.startup.gwangsan.global.security.jwt.JwtProvider;
import team.startup.gwangsan.global.security.jwt.TokenParser;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AppVersionIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.docker.compose.enabled=false", "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/migration/V10__create_app_version.sql",
        "logging.level.team.startup.gwangsan.global.filter.RequestLogFilter=OFF"})
@Testcontainers
@DisplayName("앱 버전 실제 HTTP/MariaDB 계약")
class AppVersionIntegrationTest {
    @Container
    static final MariaDBContainer<?> db = new MariaDBContainer<>("mariadb:11.4");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", db::getJdbcUrl);
        registry.add("spring.datasource.username", db::getUsername);
        registry.add("spring.datasource.password", db::getPassword);
        registry.add("spring.datasource.driver-class-name", db::getDriverClassName);
    }

    @Configuration
    @EnableAutoConfiguration
    @EntityScan("team.startup.gwangsan.domain.app.entity")
    @EnableJpaRepositories("team.startup.gwangsan.domain.app.repository")
    @Import({AppVersionController.class, FindAppVersionServiceImpl.class, SecurityConfig.class,
            JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
    static class Config {}

    @MockitoBean JwtProvider jwt;
    @MockitoBean TokenParser parser;
    @MockitoBean MemberDetailsService members;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM tbl_app_version");
        jdbc.update("INSERT INTO tbl_app_version VALUES ('ios', '1.10.0', '1.9.0'), ('android', '2.0.0', '1.0.0')");
    }

    @Nested
    @DisplayName("무인증 조회와 재배포 없는 운영 변경")
    class Read {
        @Test
        void it_returns_platform_specific_versions_and_observes_database_updates() throws Exception {
            assertVersion("ios", "1.10.0", "1.9.0");
            assertVersion("android", "2.0.0", "1.0.0");
            jdbc.update("UPDATE tbl_app_version SET latest_version = '1.11.0' WHERE platform = 'ios'");
            assertVersion("ios", "1.11.0", "1.9.0");
            assertVersion("android", "2.0.0", "1.0.0");
            jdbc.update("UPDATE tbl_app_version SET latest_version = '1.10.0' WHERE platform = 'ios'");
            assertVersion("ios", "1.10.0", "1.9.0");
        }

        @Test
        void it_rejects_missing_empty_or_unsupported_platforms() throws Exception {
            for (String query : new String[]{"", "?platform=", "?platform=IOS", "?platform=web", "?platform=%20ios"}) {
                assertThat(get("/api/app/version" + query).statusCode()).as(query).isEqualTo(400);
            }
        }

        @Test
        void it_keeps_adjacent_endpoints_and_writes_protected() throws Exception {
            assertThat(get("/api/member").statusCode()).isEqualTo(401);
            HttpRequest request = HttpRequest.newBuilder(uri("/api/app/version?platform=ios"))
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("설정 누락 및 잘못된 운영 설정")
    class ConfigurationErrors {
        @Test
        void it_returns_503_without_inventing_a_version() throws Exception {
            jdbc.update("DELETE FROM tbl_app_version WHERE platform = 'ios'");
            assertUnavailable();
            assertVersion("android", "2.0.0", "1.0.0");
        }

        @Test
        void it_rejects_malformed_or_inverted_versions() throws Exception {
            for (String version : new String[]{"1.0", "01.10.0", "1.0.0-beta", "1000000000.0.0", "1.8.0"}) {
                jdbc.update("UPDATE tbl_app_version SET latest_version = ? WHERE platform = 'ios'", version);
                assertUnavailable();
            }
            jdbc.update("UPDATE tbl_app_version SET latest_version = '1.10.0', minimum_version = 'bad' WHERE platform = 'ios'");
            assertUnavailable();
            jdbc.update("UPDATE tbl_app_version SET minimum_version = '1.10.0' WHERE platform = 'ios'");
            assertVersion("ios", "1.10.0", "1.10.0");
        }
    }

    private void assertVersion(String platform, String latest, String minimum) throws Exception {
        HttpResponse<String> response = get("/api/app/version?platform=" + platform);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("cache-control")).contains("no-store");
        assertThat(mapper.readTree(response.body())).isEqualTo(mapper.readTree(
                "{\"latestVersion\":\"" + latest + "\",\"minimumVersion\":\"" + minimum + "\"}"));
    }

    private void assertUnavailable() throws Exception {
        HttpResponse<String> response = get("/api/app/version?platform=ios");
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(mapper.readTree(response.body()).get("status").asInt()).isEqualTo(503);
        assertThat(mapper.readTree(response.body()).get("message").asText()).isEqualTo("앱 버전 정보가 아직 준비되지 않았습니다.");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
