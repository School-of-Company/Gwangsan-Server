package team.startup.gwangsan.infrastructure.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RequestLogFilter 단위 테스트")
class RequestLogFilterTest {

    private static final String FICTIONAL_SECRET_MARKER = "fictional-secret-marker";
    private Level previousLogLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        previousLogLevel = logger().getLevel();
        logger().setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger().addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger().detachAppender(appender);
        appender.stop();
        logger().setLevel(previousLogLevel);
    }

    @Nested
    @DisplayName("doFilterInternal()은")
    class Describe_doFilterInternal {

        @Test
        @DisplayName("it_요청·응답 본문, query string, header, client IP 를 로그에 남기지 않는다")
        void it_does_not_log_bodies_or_identifiers() throws Exception {
            String requestBody = "{\"content\":\"" + FICTIONAL_SECRET_MARKER + "\",\"name\":\"tester\"}";
            String responseBody = "{\"accessToken\":\"" + FICTIONAL_SECRET_MARKER + "\"}";
            MockHttpServletRequest request = jsonRequest(requestBody);
            request.setQueryString("token=" + FICTIONAL_SECRET_MARKER);
            request.addHeader("Authorization", "Bearer " + FICTIONAL_SECRET_MARKER);
            request.addHeader("User-Agent", FICTIONAL_SECRET_MARKER);
            request.setRemoteAddr("203.0.113.9");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = (wrappedRequest, wrappedResponse) -> {
                assertThat(new String(wrappedRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                        .isEqualTo(requestBody);
                wrappedResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                wrappedResponse.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
            };

            new RequestLogFilter().doFilterInternal(request, response, chain);

            ILoggingEvent event = onlyEvent(Level.INFO);
            assertThat(response.getContentAsString()).isEqualTo(responseBody);
            assertThat(event.getFormattedMessage() + event.getKeyValuePairs())
                    .doesNotContain(FICTIONAL_SECRET_MARKER)
                    .doesNotContain("203.0.113.9");
            assertThat(event.getFormattedMessage()).isEqualTo("POST /api/logs -> 200");
        }

        @Test
        @DisplayName("it_method, path, status, 소요시간을 구조화 필드로 남긴다")
        void it_logs_structured_fields() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/post;jsessionid=" + FICTIONAL_SECRET_MARKER);
            MockHttpServletResponse response = new MockHttpServletResponse();

            new RequestLogFilter().doFilterInternal(request, response,
                    (req, res) -> ((HttpServletResponse) res).setStatus(204));

            Map<String, Object> fields = fields(onlyEvent(Level.INFO));
            assertThat(fields).containsEntry("event", "http_request")
                    .containsEntry("http_method", "GET")
                    .containsEntry("http_path", "/api/post")
                    .containsEntry("http_status", 204)
                    .containsKey("duration_ms");
        }

        @Test
        @DisplayName("it_2000자를 넘는 응답 본문도 클라이언트에는 전부 전달한다")
        void it_passes_large_body_through() throws Exception {
            String responseBody = "x".repeat(2001);
            MockHttpServletResponse response = new MockHttpServletResponse();

            new RequestLogFilter().doFilterInternal(new MockHttpServletRequest("GET", "/api/logs"), response,
                    (req, res) -> res.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8)));

            assertThat(response.getContentAsString()).isEqualTo(responseBody);
            assertThat(onlyEvent(Level.INFO).getFormattedMessage()).doesNotContain("xxxx");
        }

        @Test
        @DisplayName("it_체인 예외를 그대로 전파하고 예외 타입만 ERROR 로 남긴다")
        void it_propagates_chain_exception() throws Exception {
            ServletException expected = new ServletException(FICTIONAL_SECRET_MARKER);
            MockHttpServletRequest request = jsonRequest("{\"operation\":\"fail\"}");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = (wrappedRequest, wrappedResponse) -> {
                ((HttpServletResponse) wrappedResponse).setStatus(503);
                throw expected;
            };

            assertThatThrownBy(() -> new RequestLogFilter().doFilterInternal(request, response, chain))
                    .isSameAs(expected);

            ILoggingEvent event = onlyEvent(Level.ERROR);
            assertThat(fields(event)).containsEntry("http_status", 503)
                    .containsEntry("error_type", "jakarta.servlet.ServletException");
            assertThat(event.getFormattedMessage() + event.getKeyValuePairs()).doesNotContain(FICTIONAL_SECRET_MARKER);
            assertThat(event.getThrowableProxy()).isNull();
        }
    }

    private MockHttpServletRequest jsonRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/logs");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private ILoggingEvent onlyEvent(Level level) {
        List<ILoggingEvent> events = appender.list.stream().filter(e -> e.getLevel() == level).toList();
        assertThat(events).hasSize(1);
        return events.getFirst();
    }

    private Map<String, Object> fields(ILoggingEvent event) {
        assertThat(event.getKeyValuePairs()).isNotNull();
        return event.getKeyValuePairs().stream().collect(Collectors.toMap(p -> p.key, p -> p.value));
    }

    private Logger logger() {
        return (Logger) LoggerFactory.getLogger(RequestLogFilter.class);
    }
}
