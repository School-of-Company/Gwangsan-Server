package team.startup.gwangsan.global.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    @BeforeEach
    void enableInfoLogging() {
        previousLogLevel = logger().getLevel();
        logger().setLevel(Level.INFO);
    }

    @AfterEach
    void restoreLogLevel() {
        logger().setLevel(previousLogLevel);
    }

    @Nested
    @DisplayName("doFilterInternal()은")
    class Describe_doFilterInternal {

        @Test
        @DisplayName("it_요청 본문을 체인에 전달하고 민감 필드를 가린 뒤 응답 본문을 그대로 복사한다")
        void it_passes_request_body_masks_sensitive_fields_and_copies_response_body() throws Exception {
            String requestBody = "{\"password\":\"" + FICTIONAL_SECRET_MARKER + "\",\"name\":\"tester\"}";
            String responseBody = "{\"accessToken\":\"" + FICTIONAL_SECRET_MARKER + "\",\"status\":\"ok\"}";
            MockHttpServletRequest request = jsonRequest(requestBody);
            MockHttpServletResponse response = new MockHttpServletResponse();
            ListAppender<ILoggingEvent> appender = attachAppender();
            FilterChain chain = (wrappedRequest, wrappedResponse) -> {
                assertThat(new String(wrappedRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                        .isEqualTo(requestBody);
                wrappedResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                wrappedResponse.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
            };

            try {
                new RequestLogFilter().doFilterInternal(request, response, chain);

                String message = infoLog(appender);
                assertThat(response.getContentAsString()).isEqualTo(responseBody);
                assertThat(message)
                        .doesNotContain(FICTIONAL_SECRET_MARKER)
                        .contains("\"password\":\"****\"")
                        .contains("\"accessToken\":\"****\"");
            } finally {
                detachAppender(appender);
            }
        }

        @Test
        @DisplayName("it_빈 JSON 요청과 잘못된 JSON 응답도 본문을 보존해 기록한다")
        void it_logs_blank_and_malformed_json_without_rejecting_the_response() throws Exception {
            String blankRequestBody = "   ";
            String malformedResponseBody = "{\"state\":\"fictional-json-marker\"";
            MockHttpServletRequest request = jsonRequest(blankRequestBody);
            MockHttpServletResponse response = new MockHttpServletResponse();
            ListAppender<ILoggingEvent> appender = attachAppender();
            FilterChain chain = (wrappedRequest, wrappedResponse) -> {
                assertThat(new String(wrappedRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                        .isEqualTo(blankRequestBody);
                wrappedResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                wrappedResponse.getOutputStream().write(malformedResponseBody.getBytes(StandardCharsets.UTF_8));
            };

            try {
                new RequestLogFilter().doFilterInternal(request, response, chain);

                String message = infoLog(appender);
                assertThat(response.getContentAsString()).isEqualTo(malformedResponseBody);
                assertThat(message)
                        .contains("requestBody= responseBody=")
                        .contains(malformedResponseBody);
            } finally {
                detachAppender(appender);
            }
        }

        @Test
        @DisplayName("it_2000자를 넘는 응답 본문은 로그에서만 절단하고 클라이언트에는 전부 전달한다")
        void it_truncates_large_body_only_in_the_log() throws Exception {
            String responseBody = "x".repeat(2001);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/logs");
            MockHttpServletResponse response = new MockHttpServletResponse();
            ListAppender<ILoggingEvent> appender = attachAppender();
            FilterChain chain = (wrappedRequest, wrappedResponse) -> {
                assertThat(wrappedRequest.getInputStream().readAllBytes()).isEmpty();
                wrappedResponse.setContentType(MediaType.TEXT_PLAIN_VALUE);
                wrappedResponse.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
            };

            try {
                new RequestLogFilter().doFilterInternal(request, response, chain);

                String message = infoLog(appender);
                assertThat(response.getContentAsString()).isEqualTo(responseBody);
                assertThat(message)
                        .contains("responseBody=" + "x".repeat(2000))
                        .contains("...(truncated, total 2001 chars)")
                        .doesNotContain(responseBody);
            } finally {
                detachAppender(appender);
            }
        }

        @Test
        @DisplayName("it_체인 예외를 그대로 전파하면서 작성된 응답 본문은 복사한다")
        void it_propagates_chain_exception_and_copies_written_response_body() throws Exception {
            String requestBody = "{\"operation\":\"fail\"}";
            String responseBody = "{\"result\":\"fictional-error-response\"}";
            ServletException expected = new ServletException("expected chain failure");
            MockHttpServletRequest request = jsonRequest(requestBody);
            MockHttpServletResponse response = new MockHttpServletResponse();
            ListAppender<ILoggingEvent> appender = attachAppender();
            FilterChain chain = (wrappedRequest, wrappedResponse) -> {
                assertThat(new String(wrappedRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                        .isEqualTo(requestBody);
                ((HttpServletResponse) wrappedResponse).setStatus(503);
                wrappedResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                wrappedResponse.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
                throw expected;
            };

            try {
                assertThatThrownBy(() -> new RequestLogFilter().doFilterInternal(request, response, chain))
                        .isSameAs(expected);

                assertThat(response.getStatus()).isEqualTo(503);
                assertThat(response.getContentAsString()).isEqualTo(responseBody);
                assertThat(infoLog(appender)).contains(responseBody);
            } finally {
                detachAppender(appender);
            }
        }
    }

    private MockHttpServletRequest jsonRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/logs");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger().addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        logger().detachAppender(appender);
        appender.stop();
    }

    private String infoLog(ListAppender<ILoggingEvent> appender) {
        List<String> messages = appender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(messages).hasSize(1);
        return messages.getFirst();
    }

    private Logger logger() {
        return (Logger) LoggerFactory.getLogger(RequestLogFilter.class);
    }
}
