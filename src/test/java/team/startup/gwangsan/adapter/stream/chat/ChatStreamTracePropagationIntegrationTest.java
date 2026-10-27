package team.startup.gwangsan.adapter.stream.chat;

import com.google.gson.Gson;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
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
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import team.startup.gwangsan.application.chat.port.in.SaveChatMessageUseCase;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * 실제 Redis stream 을 거쳐 retry·reclaim·DLQ 가 생성 시점 trace context 를 이어 가는지 본다.
 */
@ExtendWith(MockitoExtension.class)
@Testcontainers
@DisplayName("chat stream trace context 전파 통합 테스트")
class ChatStreamTracePropagationIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final String STREAM_KEY = "chat:room:77:messages";
    private static final String RETRY_KEY = "chat:retry:messages";
    private static final String DLQ_KEY = "chat:dlq:messages";
    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PRODUCER_SPAN_ID = "00f067aa0ba902b7";
    private static final String TRACEPARENT = "00-" + TRACE_ID + "-" + PRODUCER_SPAN_ID + "-01";
    private static final String TRACESTATE = "gwangsan=chat";

    @Mock
    private SaveChatMessageUseCase saveChatMessageUseCase;

    private final InMemorySpanExporter spans = InMemorySpanExporter.create();
    private SdkTracerProvider tracerProvider;
    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private ChatStreamProperties props;
    private ChatStreamRedisAdapter adapter;
    private ChatStreamConsumerWorker worker;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        props = new ChatStreamProperties();
        props.setGroup("chat-message-persistors");
        props.setBatchSize(10);
        props.setClaimIdleMs(0);
        props.setRetryMax(5);
        props.setRetryKey(RETRY_KEY);
        props.setDlqKey(DLQ_KEY);

        tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(spans)).build();
        ChatStreamTelemetry telemetry = new ChatStreamTelemetry(
                OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build(), new SimpleMeterRegistry());
        Gson gson = new Gson();
        adapter = new ChatStreamRedisAdapter(redisTemplate, props, gson);
        adapter.init();
        worker = new ChatStreamConsumerWorker(adapter,
                new ChatStreamMessageProcessor(saveChatMessageUseCase, adapter, props, gson, telemetry), props);
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
        connectionFactory.destroy();
    }

    @Nested
    @DisplayName("재시도는")
    class Describe_retry {

        @Test
        @DisplayName("it_retry record 에 원래 traceparent 를 옮기고 각 시도 span 이 producer span 의 자식이 된다")
        void it_keeps_creation_context_across_retry() {
            doThrow(new IllegalStateException("db down")).doReturn(null).when(saveChatMessageUseCase).execute(any());
            publish(Map.of("traceparent", TRACEPARENT, "tracestate", TRACESTATE));

            worker.consumeMessages();
            Map<String, String> retryRecord = onlyRecord(RETRY_KEY);
            worker.consumeMessages();

            assertThat(retryRecord).containsEntry("traceparent", TRACEPARENT).containsEntry("tracestate", TRACESTATE);
            List<SpanData> attempts = consumerSpans();
            assertThat(attempts).hasSize(2);
            assertThat(attempts).allSatisfy(span -> {
                assertThat(span.getTraceId()).isEqualTo(TRACE_ID);
                assertThat(span.getParentSpanId()).isEqualTo(PRODUCER_SPAN_ID);
            });
            assertThat(attempts.get(0).getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
            assertThat(attempts.get(1).getStatus().getStatusCode()).isEqualTo(StatusCode.UNSET);
            assertThat(attempts.get(1).getAttributes().asMap().toString()).contains("retry");
        }

        @Test
        @DisplayName("it_traceparent 없는 레거시 record 는 retry 에도 trace 필드를 만들어 넣지 않는다")
        void it_does_not_inject_failed_attempt_context() {
            doThrow(new IllegalStateException("db down")).when(saveChatMessageUseCase).execute(any());
            publish(Map.of());

            worker.consumeMessages();

            assertThat(onlyRecord(RETRY_KEY)).doesNotContainKeys("traceparent", "tracestate");
        }
    }

    @Nested
    @DisplayName("DLQ 는")
    class Describe_dlq {

        @Test
        @DisplayName("it_원래 traceparent/tracestate 를 최상위 필드로 남긴다")
        void it_propagates_creation_context_to_dlq() {
            doThrow(new NotFoundChatRoomException()).when(saveChatMessageUseCase).execute(any());
            publish(Map.of("traceparent", TRACEPARENT, "tracestate", TRACESTATE));

            worker.consumeMessages();

            assertThat(onlyRecord(DLQ_KEY)).containsEntry("traceparent", TRACEPARENT)
                    .containsEntry("tracestate", TRACESTATE);
            assertThat(consumerSpans().getFirst().getParentSpanId()).isEqualTo(PRODUCER_SPAN_ID);
        }
    }

    @Nested
    @DisplayName("reclaim 은")
    class Describe_reclaim {

        @Test
        @DisplayName("it_다시 가져온 pending record 도 생성 context 아래 새 시도 span 으로 처리한다")
        void it_parents_reclaimed_attempt_on_creation_context() {
            publish(Map.of("traceparent", TRACEPARENT));
            adapter.refreshStreamKeys();
            adapter.ensureGroupExists(STREAM_KEY);
            assertThat(adapter.readMessages(STREAM_KEY)).hasSize(1); // 처리 전 중단된 소비자를 흉내 낸다

            worker.reclaimPendingMessages();

            assertThat(consumerSpans()).singleElement().satisfies(span -> {
                assertThat(span.getParentSpanId()).isEqualTo(PRODUCER_SPAN_ID);
                assertThat(span.getTraceId()).isEqualTo(TRACE_ID);
            });
            assertThat(redisTemplate.opsForStream().pending(STREAM_KEY, props.getGroup(), Range.unbounded(), 10L).size())
                    .isZero();
        }
    }

    private void publish(Map<String, String> traceFields) {
        Map<String, String> body = new HashMap<>(Map.of("messageId", "9001", "roomId", "77", "senderId", "7",
                "content", "fictional", "messageType", "TEXT", "createdAt", String.valueOf(System.currentTimeMillis())));
        body.putAll(traceFields);
        redisTemplate.opsForStream().add(StreamRecords.newRecord().in(STREAM_KEY).ofMap(body));
    }

    private Map<String, String> onlyRecord(String key) {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().range(key, Range.unbounded());
        assertThat(records).hasSize(1);
        Map<String, String> values = new HashMap<>();
        records.getFirst().getValue().forEach((k, v) -> values.put(String.valueOf(k), String.valueOf(v)));
        return values;
    }

    private List<SpanData> consumerSpans() {
        return spans.getFinishedSpanItems().stream().filter(s -> s.getKind() == SpanKind.CONSUMER).toList();
    }
}
