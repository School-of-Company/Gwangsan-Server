package team.startup.gwangsan.adapter.stream.chat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.gson.Gson;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import team.startup.gwangsan.application.chat.port.in.SaveChatMessageUseCase;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatStreamMessageProcessor tracing·metric·로그 개인정보")
class ChatStreamProcessorTelemetryTest {

    private static final String STREAM_KEY = "chat:room:42:messages";
    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PARENT_SPAN_ID = "00f067aa0ba902b7";
    private static final String TRACEPARENT = "00-" + TRACE_ID + "-" + PARENT_SPAN_ID + "-01";
    private static final String CONTENT_MARKER = "fictional-chat-body-marker";

    @Mock
    private SaveChatMessageUseCase saveChatMessageUseCase;
    @Mock
    private ChatStreamRedisAdapter redisAdapter;

    private final InMemorySpanExporter spans = InMemorySpanExporter.create();
    private SdkTracerProvider tracerProvider;
    private SimpleMeterRegistry meters;
    private ChatStreamMessageProcessor processor;

    @BeforeEach
    void setUp() {
        tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spans)).build();
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
        meters = new SimpleMeterRegistry();
        ChatStreamProperties props = new ChatStreamProperties();
        props.setGroup("chat-message-persistors");
        props.setRetryKey("chat:retry:messages");
        props.setDlqKey("chat:dlq:messages");
        props.setRetryMax(5);
        processor = new ChatStreamMessageProcessor(saveChatMessageUseCase, redisAdapter, props, new Gson(),
                new ChatStreamTelemetry(sdk, meters));
        lenient().when(redisAdapter.ack(anyString(), any())).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
        MDC.clear();
    }

    @Nested
    @DisplayName("trace context 추출은")
    class Describe_extraction {

        @Test
        @DisplayName("it_traceparent 가 있으면 그 span 을 부모로 한 CONSUMER span 을 만든다")
        void it_creates_consumer_span_under_producer_context() {
            processor.process(STREAM_KEY, record(Map.of("traceparent", TRACEPARENT)), 0);

            SpanData span = onlySpan();
            assertThat(span.getKind()).isEqualTo(SpanKind.CONSUMER);
            assertThat(span.getName()).isEqualTo("chat-stream process");
            assertThat(span.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(span.getParentSpanId()).isEqualTo(PARENT_SPAN_ID);
            assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
            assertThat(span.getAttributes().asMap().toString()).contains("committed");
        }

        @Test
        @DisplayName("it_traceparent 가 없는 레거시 record 는 새 root span 으로 저장·ACK 한다")
        void it_handles_legacy_record_as_root() {
            processor.process(STREAM_KEY, record(Map.of()), 0);

            assertThat(onlySpan().getParentSpanContext().isValid()).isFalse();
            verify(saveChatMessageUseCase).execute(any());
            verify(redisAdapter).ack(STREAM_KEY, RecordId.of("1700000000000-0"));
        }

        @Test
        @DisplayName("it_잘못된 traceparent 는 무시하고 업무 결과를 바꾸지 않는다")
        void it_ignores_invalid_traceparent() {
            processor.process(STREAM_KEY, record(Map.of("traceparent", "garbage", "tracestate", "x=1")), 0);

            assertThat(onlySpan().getParentSpanContext().isValid()).isFalse();
            verify(saveChatMessageUseCase).execute(any());
            verify(redisAdapter).ack(STREAM_KEY, RecordId.of("1700000000000-0"));
            verify(redisAdapter, never()).sendToDlq(any(), any(), any());
        }

        @Test
        @DisplayName("it_현재 스케줄러 context 가 아니라 record context 만 부모로 쓴다")
        void it_does_not_inherit_ambient_context() {
            Span ambient = tracerProvider.get("test").spanBuilder("ambient").startSpan();
            try (Scope ignored = ambient.makeCurrent()) {
                processor.process(STREAM_KEY, record(Map.of()), 0);
            } finally {
                ambient.end();
            }

            SpanData consumer = spans.getFinishedSpanItems().stream()
                    .filter(s -> s.getKind() == SpanKind.CONSUMER).findFirst().orElseThrow();
            assertThat(consumer.getTraceId()).isNotEqualTo(ambient.getSpanContext().getTraceId());
        }
    }

    @Nested
    @DisplayName("context 격리는")
    class Describe_isolation {

        @Test
        @DisplayName("it_저장 중에는 consumer span 이 현재 context 이고 끝나면 이전 context·MDC 로 돌아간다")
        void it_scopes_context_to_the_attempt() {
            AtomicReference<SpanContext> during = new AtomicReference<>();
            doAnswer(invocation -> {
                during.set(Span.current().getSpanContext());
                return null;
            }).when(saveChatMessageUseCase).execute(any());
            MDC.put("request_marker", "kept");
            Context before = Context.current();

            processor.process(STREAM_KEY, record(Map.of("traceparent", TRACEPARENT)), 0);

            assertThat(during.get().getSpanId()).isEqualTo(onlySpan().getSpanId());
            assertThat(Context.current()).isSameAs(before);
            assertThat(MDC.get("request_marker")).isEqualTo("kept");
        }

        @Test
        @DisplayName("it_인계 쓰기 예외로 빠져나가도 이전 context 로 돌아가고 span 을 ERROR 로 닫는다")
        void it_restores_context_when_handoff_fails() {
            doThrow(new IllegalStateException("retry write failed")).when(saveChatMessageUseCase).execute(any());
            doThrow(new IllegalStateException("xadd failed")).when(redisAdapter).sendToRetry(any(), any(), anyInt(), any());
            MDC.put("request_marker", "kept");
            Context before = Context.current();

            assertThatThrownBy(() -> processor.process(STREAM_KEY, record(Map.of("traceparent", TRACEPARENT)), 0))
                    .hasMessage("xadd failed");

            assertThat(Context.current()).isSameAs(before);
            assertThat(MDC.get("request_marker")).isEqualTo("kept");
            SpanData span = onlySpan();
            assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
            assertThat(span.getAttributes().asMap().toString()).contains("handoff_failed");
            verify(redisAdapter, never()).ack(any(), any());
            assertThat(count("gwangsan.chat.retry", "failure")).isEqualTo(1);
            assertThat(count("gwangsan.chat.ack", "success")).isZero();
        }
    }

    @Nested
    @DisplayName("잡힌 실패는")
    class Describe_caught_errors {

        @Test
        @DisplayName("it_재시도 대상 실패를 span ERROR 와 outcome metric 으로 남기고 예외 메시지는 남기지 않는다")
        void it_records_transient_failure() {
            doThrow(new IllegalStateException(CONTENT_MARKER)).when(saveChatMessageUseCase).execute(any());

            processor.process(STREAM_KEY, record(Map.of("traceparent", TRACEPARENT)), 0);

            SpanData span = onlySpan();
            assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
            assertThat(span.getAttributes().asMap().toString())
                    .contains("java.lang.IllegalStateException").contains("retry_scheduled")
                    .doesNotContain(CONTENT_MARKER);
            assertThat(span.getEvents()).isEmpty();
            assertThat(count("gwangsan.chat.persist.attempts", "transient_failure")).isEqualTo(1);
            assertThat(meters.get("gwangsan.chat.persist.duration").tag("outcome", "failure").timer().count()).isEqualTo(1);
            assertThat(count("gwangsan.chat.retry", "success")).isEqualTo(1);
            assertThat(count("gwangsan.chat.ack", "success")).isEqualTo(1);
            assertThat(meters.get("gwangsan.chat.publish_to_commit").timer().count()).isZero();
        }

        @Test
        @DisplayName("it_영구 실패는 DLQ 성공과 permanent_failure 로 센다")
        void it_records_permanent_failure() {
            doThrow(new NotFoundChatRoomException()).when(saveChatMessageUseCase).execute(any());

            processor.process(STREAM_KEY, record(Map.of()), 0);

            assertThat(count("gwangsan.chat.persist.attempts", "permanent_failure")).isEqualTo(1);
            assertThat(count("gwangsan.chat.dlq", "success")).isEqualTo(1);
            assertThat(onlySpan().getAttributes().asMap().toString()).contains("dead_lettered");
        }
    }

    @Nested
    @DisplayName("성공·ACK metric 은")
    class Describe_metrics {

        @Test
        @DisplayName("it_커밋 성공에만 success 와 publish_to_commit 을 기록한다")
        void it_records_commit() {
            processor.process(STREAM_KEY, record(Map.of()), 0);

            assertThat(count("gwangsan.chat.persist.attempts", "success")).isEqualTo(1);
            assertThat(meters.get("gwangsan.chat.persist.duration").tag("outcome", "success").timer().count()).isEqualTo(1);
            assertThat(meters.get("gwangsan.chat.publish_to_commit").timer().count()).isEqualTo(1);
            assertThat(count("gwangsan.chat.ack", "success")).isEqualTo(1);
        }

        @Test
        @DisplayName("it_XACK 가 실패하면 성공으로 세지 않는다")
        void it_counts_failed_ack_as_failure() {
            when(redisAdapter.ack(anyString(), any())).thenReturn(null);

            processor.process(STREAM_KEY, record(Map.of()), 0);

            assertThat(count("gwangsan.chat.ack", "failure")).isEqualTo(1);
            assertThat(count("gwangsan.chat.ack", "success")).isZero();
        }

        @Test
        @DisplayName("it_XACK 가 0 을 돌리면 not_pending 으로 센다")
        void it_counts_noop_ack_as_not_pending() {
            when(redisAdapter.ack(anyString(), any())).thenReturn(0L);

            processor.process(STREAM_KEY, record(Map.of()), 0);

            assertThat(count("gwangsan.chat.ack", "not_pending")).isEqualTo(1);
            assertThat(count("gwangsan.chat.ack", "success")).isZero();
        }

        @Test
        @DisplayName("it_DLQ 쓰기 실패는 dlq failure 로 세고 ACK 하지 않는다")
        void it_counts_dlq_failure() {
            doThrow(new IllegalStateException("xadd failed")).when(redisAdapter).sendToDlq(any(), any(), any());
            Map<String, String> invalid = new HashMap<>(validBody());
            invalid.remove("createdAt");

            assertThatThrownBy(() -> processor.process(STREAM_KEY, MapRecord.create(STREAM_KEY, invalid)
                    .withId(RecordId.of("1700000000000-0")), 0)).isInstanceOf(IllegalStateException.class);

            assertThat(count("gwangsan.chat.dlq", "failure")).isEqualTo(1);
            assertThat(count("gwangsan.chat.persist.attempts", "invalid_payload")).isEqualTo(1);
            verify(redisAdapter, never()).ack(any(), any());
        }
    }

    @Nested
    @DisplayName("counter 초기화는")
    class Describe_counter_initialization {

        @Test
        @DisplayName("it_처리 전에도 모든 outcome counter 를 0 으로 노출해 첫 burst 를 rate 로 잡게 한다")
        void it_registers_zero_counters_before_any_attempt() {
            Map<String, List<String>> required = Map.of(
                    "gwangsan.chat.persist.attempts",
                    List.of("success", "transient_failure", "permanent_failure", "invalid_payload"),
                    "gwangsan.chat.ack", List.of("success", "failure", "not_pending"),
                    "gwangsan.chat.retry", List.of("success", "failure"),
                    "gwangsan.chat.dlq", List.of("success", "failure"));

            required.forEach((name, outcomes) -> {
                assertThat(meters.find(name).counters()).hasSize(outcomes.size());
                outcomes.forEach(outcome ->
                        assertThat(meters.get(name).tag("outcome", outcome).counter().count()).isZero());
            });
        }
    }

    @Nested
    @DisplayName("로그는")
    class Describe_logging_privacy {

        @Test
        @DisplayName("it_매핑 실패 로그에 채팅 본문과 예외 메시지를 남기지 않는다")
        void it_does_not_log_payload_on_mapping_failure() {
            Map<String, String> invalid = new HashMap<>(validBody());
            invalid.put("content", CONTENT_MARKER);
            invalid.put("senderId", CONTENT_MARKER); // NumberFormatException 메시지에 입력값이 담긴다
            ListAppender<ILoggingEvent> appender = attach();
            try {
                processor.process(STREAM_KEY, MapRecord.create(STREAM_KEY, invalid).withId(RecordId.of("1-0")), 0);
            } finally {
                logger().detachAppender(appender);
            }

            assertThat(appender.list).isNotEmpty();
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain(CONTENT_MARKER);
                assertThat(String.valueOf(event.getKeyValuePairs())).doesNotContain(CONTENT_MARKER)
                        .contains("chat_stream_invalid_payload");
                assertThat(event.getThrowableProxy()).isNull();
            });
        }

        @Test
        @DisplayName("it_재시도 로그에 예외 메시지 대신 예외 타입만 남긴다")
        void it_logs_error_type_only_on_retry() {
            doThrow(new IllegalStateException(CONTENT_MARKER)).when(saveChatMessageUseCase).execute(any());
            ListAppender<ILoggingEvent> appender = attach();
            try {
                processor.process(STREAM_KEY, record(Map.of()), 0);
            } finally {
                logger().detachAppender(appender);
            }

            ILoggingEvent event = appender.list.getFirst();
            assertThat(event.getFormattedMessage()).doesNotContain(CONTENT_MARKER);
            assertThat(String.valueOf(event.getKeyValuePairs()))
                    .doesNotContain(CONTENT_MARKER).contains("java.lang.IllegalStateException");
        }

        private ListAppender<ILoggingEvent> attach() {
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger().addAppender(appender);
            return appender;
        }

        private Logger logger() {
            return (Logger) LoggerFactory.getLogger(ChatStreamMessageProcessor.class);
        }
    }

    private SpanData onlySpan() {
        List<SpanData> finished = spans.getFinishedSpanItems();
        assertThat(finished).hasSize(1);
        return finished.getFirst();
    }

    private double count(String name, String outcome) {
        var counter = meters.find(name).tag("outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }

    private static Map<String, String> validBody() {
        return Map.of("messageId", "999", "roomId", "42", "senderId", "7", "content", CONTENT_MARKER,
                "messageType", "TEXT", "createdAt", "1700000000000");
    }

    private static MapRecord<String, String, String> record(Map<String, String> traceFields) {
        Map<String, String> body = new HashMap<>(validBody());
        body.putAll(traceFields);
        return MapRecord.create(STREAM_KEY, body).withId(RecordId.of("1700000000000-0"));
    }
}
