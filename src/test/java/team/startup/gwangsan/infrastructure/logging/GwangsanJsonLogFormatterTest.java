package team.startup.gwangsan.infrastructure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.KeyValuePair;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GwangsanJsonLogFormatter")
class GwangsanJsonLogFormatterTest {

    private static final String SECRET = "fictional-secret-marker";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Logger logger = (Logger) LoggerFactory.getLogger("team.startup.gwangsan.Sample");
    private final GwangsanJsonLogFormatter formatter =
            new GwangsanJsonLogFormatter(new MockEnvironment().withProperty("spring.application.name", "gwangsan-api"));

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Nested
    @DisplayName("format()은")
    class Describe_format {

        @Test
        @DisplayName("it_한 줄 JSON 에 timestamp, level, service, message 를 담는다")
        void it_writes_required_fields() throws Exception {
            String line = formatter.format(event("저장 완료", null));

            assertThat(line).endsWith("\n").doesNotContain("\n{");
            Map<String, Object> json = parse(line);
            assertThat(json).containsEntry("level", "INFO")
                    .containsEntry("service", "gwangsan-api")
                    .containsEntry("message", "저장 완료")
                    .containsEntry("logger", "team.startup.gwangsan.Sample")
                    .containsKey("timestamp")
                    .doesNotContainKeys("trace_id", "span_id", "error_type");
        }

        @Test
        @DisplayName("it_agent 가 MDC 에 넣은 trace_id/span_id 를 쓰고 다른 MDC 값은 내보내지 않는다")
        void it_uses_mdc_trace_fields_only() throws Exception {
            MDC.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
            MDC.put("span_id", "00f067aa0ba902b7");
            MDC.put("member_phone", SECRET);

            Map<String, Object> json = parse(formatter.format(event("m", null)));

            assertThat(json).containsEntry("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736")
                    .containsEntry("span_id", "00f067aa0ba902b7");
            assertThat(json.toString()).doesNotContain(SECRET);
        }

        @Test
        @DisplayName("it_MDC 가 비어 있으면 현재 OTel span 의 trace_id/span_id 를 쓴다")
        void it_falls_back_to_current_span() throws Exception {
            try (SdkTracerProvider provider = SdkTracerProvider.builder().build()) {
                Span span = provider.get("test").spanBuilder("s").startSpan();
                Map<String, Object> json;
                try (Scope ignored = span.makeCurrent()) {
                    json = parse(formatter.format(event("m", null)));
                } finally {
                    span.end();
                }
                assertThat(json).containsEntry("trace_id", span.getSpanContext().getTraceId())
                        .containsEntry("span_id", span.getSpanContext().getSpanId());
            }
        }

        @Test
        @DisplayName("it_예외는 타입과 frame 만 남기고 메시지(payload)는 버린다")
        void it_strips_exception_messages() throws Exception {
            RuntimeException error = new IllegalStateException("content=" + SECRET,
                    new NumberFormatException("For input string: \"" + SECRET + "\""));

            Map<String, Object> json = parse(formatter.format(event("실패", error)));

            assertThat(json).containsEntry("error_type", "java.lang.IllegalStateException");
            assertThat((String) json.get("stack_trace"))
                    .contains("java.lang.IllegalStateException")
                    .contains("Caused by: java.lang.NumberFormatException")
                    .contains("\tat ");
            assertThat(json.toString()).doesNotContain(SECRET);
        }

        @Test
        @DisplayName("it_fluent API key-value 를 최상위 필드로 쓰되 예약 필드는 덮어쓰지 못한다")
        void it_writes_key_values() throws Exception {
            LoggingEvent event = event("m", null);
            event.addKeyValuePair(new KeyValuePair("event", "http_request"));
            event.addKeyValuePair(new KeyValuePair("http_status", 200));
            event.addKeyValuePair(new KeyValuePair("service", "spoofed"));

            Map<String, Object> json = parse(formatter.format(event));

            assertThat(json).containsEntry("event", "http_request")
                    .containsEntry("http_status", "200")
                    .containsEntry("service", "gwangsan-api");
        }
    }

    private LoggingEvent event(String message, Throwable error) {
        LoggingEvent event = new LoggingEvent(Logger.class.getName(), logger, Level.INFO, message, error, null);
        event.setKeyValuePairs(null);
        return event;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String line) throws Exception {
        return JSON.readValue(line, Map.class);
    }
}
