package team.startup.gwangsan.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.logging.structured.JsonWriterStructuredLogFormatter;
import org.springframework.core.env.Environment;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * logging.structured.format.console 로 지정하는 한 줄 JSON 포맷.
 * MDC 는 trace_id/span_id 만 꺼내고(Java agent 가 넣는다), agent 가 없으면 현재 OTel span 에서 읽는다.
 * 예외는 타입과 frame 만 남긴다. 예외 메시지에는 payload·SQL 값이 섞일 수 있어 기록하지 않는다.
 */
public class GwangsanJsonLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

    static final String TRACE_ID = "trace_id";
    static final String SPAN_ID = "span_id";
    private static final Set<String> RESERVED = Set.of(
            "timestamp", "level", "service", "logger", "thread", "message", TRACE_ID, SPAN_ID, "error_type", "stack_trace");
    private static final int MAX_FRAMES = 30;
    private static final int MAX_CAUSES = 10;

    public GwangsanJsonLogFormatter(Environment environment) {
        this(environment.getProperty("spring.application.name", "gwangsan-api"));
    }

    GwangsanJsonLogFormatter(String service) {
        super(members -> {
            members.add("timestamp", event -> Instant.ofEpochMilli(event.getTimeStamp()).toString());
            members.add("level", event -> event.getLevel().toString());
            members.add("service", service);
            members.add("logger", ILoggingEvent::getLoggerName);
            members.add("thread", ILoggingEvent::getThreadName);
            members.add("message", ILoggingEvent::getFormattedMessage);
            members.addMapEntries(GwangsanJsonLogFormatter::traceFields);
            members.addMapEntries(GwangsanJsonLogFormatter::keyValues);
            members.add("error_type", event -> event.getThrowableProxy() == null ? null
                    : event.getThrowableProxy().getClassName()).whenNotNull();
            members.add("stack_trace", event -> event.getThrowableProxy() == null ? null
                    : stackTrace(event.getThrowableProxy())).whenNotNull();
        }, null);
    }

    private static Map<String, String> traceFields(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        Map<String, String> fields = new LinkedHashMap<>();
        if (mdc != null && mdc.get(TRACE_ID) != null) {
            fields.put(TRACE_ID, mdc.get(TRACE_ID));
            if (mdc.get(SPAN_ID) != null) fields.put(SPAN_ID, mdc.get(SPAN_ID));
            return fields;
        }
        SpanContext context = Span.current().getSpanContext();
        if (context.isValid()) {
            fields.put(TRACE_ID, context.getTraceId());
            fields.put(SPAN_ID, context.getSpanId());
        }
        return fields;
    }

    // SLF4J fluent API(addKeyValue) 로 넣은 이벤트 필드. 호출 코드가 정한 안전한 값만 들어온다.
    private static Map<String, Object> keyValues(ILoggingEvent event) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (event.getKeyValuePairs() == null) return fields;
        for (KeyValuePair pair : event.getKeyValuePairs()) {
            if (pair.key != null && !RESERVED.contains(pair.key)) {
                fields.put(pair.key, pair.value == null ? null : String.valueOf(pair.value));
            }
        }
        return fields;
    }

    private static String stackTrace(IThrowableProxy throwable) {
        StringBuilder out = new StringBuilder();
        int depth = 0;
        for (IThrowableProxy current = throwable; current != null && depth++ < MAX_CAUSES; current = current.getCause()) {
            if (current != throwable) out.append("Caused by: ");
            out.append(current.getClassName()).append('\n');
            StackTraceElementProxy[] frames = current.getStackTraceElementProxyArray();
            int shown = Math.min(frames.length, MAX_FRAMES);
            for (int i = 0; i < shown; i++) {
                out.append("\tat ").append(frames[i].getStackTraceElement()).append('\n');
            }
            if (frames.length > shown) out.append("\t... ").append(frames.length - shown).append(" more\n");
        }
        return out.toString();
    }
}
