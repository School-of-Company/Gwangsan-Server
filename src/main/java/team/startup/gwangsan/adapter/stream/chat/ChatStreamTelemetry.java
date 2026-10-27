package team.startup.gwangsan.adapter.stream.chat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * chat stream 소비의 span·metric. 이름과 outcome 값은 .omo/observability-contract.md 의 wire 계약이다.
 * span 부모는 record 의 traceparent/tracestate(생성 시점 context)만 쓴다. baggage 는 읽지 않는다.
 */
@Component
public class ChatStreamTelemetry {

    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FAILURE = "failure";
    public static final String PERSIST_TRANSIENT_FAILURE = "transient_failure";
    public static final String PERSIST_PERMANENT_FAILURE = "permanent_failure";
    public static final String PERSIST_INVALID_PAYLOAD = "invalid_payload";
    public static final String ACK_NOT_PENDING = "not_pending";

    static final String SPAN_NAME = "chat-stream process";
    private static final Duration[] BUCKETS = {
            Duration.ofMillis(5), Duration.ofMillis(10), Duration.ofMillis(25), Duration.ofMillis(50),
            Duration.ofMillis(100), Duration.ofMillis(250), Duration.ofMillis(500), Duration.ofSeconds(1),
            Duration.ofMillis(2500), Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofSeconds(30),
            Duration.ofSeconds(60)};

    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return List.of(ChatStreamField.TRACEPARENT, ChatStreamField.TRACESTATE);
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier == null ? null : carrier.get(key);
        }
    };

    private final Tracer tracer;
    private final MeterRegistry registry;
    private final Timer publishToCommit;

    public ChatStreamTelemetry(OpenTelemetry openTelemetry, MeterRegistry registry) {
        this.tracer = openTelemetry.getTracer("team.startup.gwangsan.chat-stream");
        this.registry = registry;
        this.publishToCommit = timer("gwangsan.chat.publish_to_commit", null);
        // 첫 이벤트 전에도 0 인 series 가 있어야 increase()/rate() 가 첫 burst 를 잡는다.
        for (String outcome : List.of(OUTCOME_SUCCESS, PERSIST_TRANSIENT_FAILURE, PERSIST_PERMANENT_FAILURE, PERSIST_INVALID_PAYLOAD)) {
            counter("gwangsan.chat.persist.attempts", outcome);
        }
        for (String outcome : List.of(OUTCOME_SUCCESS, OUTCOME_FAILURE, ACK_NOT_PENDING)) {
            counter("gwangsan.chat.ack", outcome);
        }
        for (String outcome : List.of(OUTCOME_SUCCESS, OUTCOME_FAILURE)) {
            counter("gwangsan.chat.retry", outcome);
            counter("gwangsan.chat.dlq", outcome);
        }
    }

    /** 생성 context 를 부모로 한 CONSUMER span. traceparent 가 없거나 잘못되면 새 root trace 가 된다. */
    public Span startProcessSpan(String streamKey, MapRecord<String, String, String> record, int attempt, boolean retryStream) {
        Context parent = W3CTraceContextPropagator.getInstance().extract(Context.root(), record.getValue(), GETTER);
        return tracer.spanBuilder(SPAN_NAME)
                .setParent(parent)
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "redis")
                .setAttribute("messaging.operation.type", "process")
                .setAttribute("messaging.destination.name", retryStream ? streamKey : "chat:room:{roomId}:messages")
                .setAttribute("messaging.message.id", record.getId().getValue())
                .setAttribute("gwangsan.chat.attempt", attempt)
                .setAttribute("gwangsan.chat.source", retryStream ? "retry" : "primary")
                .startSpan();
    }

    /** 예외 메시지는 payload 를 담을 수 있어 span 에 남기지 않고 타입만 기록한다. */
    public void recordError(Span span, Throwable error) {
        span.setStatus(StatusCode.ERROR, error.getClass().getSimpleName());
        span.setAttribute("error.type", error.getClass().getName());
    }

    public void persistAttempt(String outcome) {
        counter("gwangsan.chat.persist.attempts", outcome).increment();
    }

    public void persistDuration(long nanos, String outcome) {
        timer("gwangsan.chat.persist.duration", outcome).record(Duration.ofNanos(nanos));
    }

    /** 생성(createdAt, epoch ms)부터 DB 커밋까지. 커밋 성공에만 호출한다. 시계 역전 값은 버린다. */
    public void publishToCommit(long createdAtEpochMillis, long nowEpochMillis) {
        long elapsed = nowEpochMillis - createdAtEpochMillis;
        if (elapsed >= 0) publishToCommit.record(Duration.ofMillis(elapsed));
    }

    /** XACK 반환값 기준: null=명령 실패, 0=이미 pending 아님, 1 이상=실제 ACK. */
    public void ack(Long acknowledged) {
        String outcome = acknowledged == null ? OUTCOME_FAILURE
                : acknowledged > 0 ? OUTCOME_SUCCESS : ACK_NOT_PENDING;
        counter("gwangsan.chat.ack", outcome).increment();
    }

    public void retry(boolean success) {
        counter("gwangsan.chat.retry", outcome(success)).increment();
    }

    public void dlq(boolean success) {
        counter("gwangsan.chat.dlq", outcome(success)).increment();
    }

    private static String outcome(boolean success) {
        return success ? OUTCOME_SUCCESS : OUTCOME_FAILURE;
    }

    private Counter counter(String name, String outcome) {
        return Counter.builder(name).tag("outcome", outcome).register(registry);
    }

    private Timer timer(String name, String outcome) {
        Timer.Builder builder = Timer.builder(name).serviceLevelObjectives(BUCKETS);
        if (outcome != null) builder.tag("outcome", outcome);
        return builder.register(registry);
    }
}
