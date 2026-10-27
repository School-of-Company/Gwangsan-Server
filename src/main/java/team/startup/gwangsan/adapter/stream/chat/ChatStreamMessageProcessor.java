package team.startup.gwangsan.adapter.stream.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.data.SaveChatMessageCommand;
import team.startup.gwangsan.application.chat.port.in.SaveChatMessageUseCase;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;
import team.startup.gwangsan.domain.chat.exception.ChatMessageIdConflictException;
import team.startup.gwangsan.domain.chat.exception.InvalidChatStreamPayloadException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatSenderException;
import team.startup.gwangsan.domain.chat.model.MessageType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamMessageProcessor {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String OUTCOME_ATTRIBUTE = "gwangsan.chat.outcome";

    private final SaveChatMessageUseCase saveChatMessageUseCase;
    private final ChatStreamRedisAdapter redisAdapter;
    private final ChatStreamProperties props;
    private final Gson gson;
    private final ChatStreamTelemetry telemetry;

    /**
     * 전달 1회(read 또는 reclaim)마다 CONSUMER span 하나를 연다. span scope 는 끝나면 이전 context 로 돌아간다(예외 포함).
     * retry/DLQ 로 넘길 때는 record 의 원래 traceparent 를 그대로 옮겨, 실패한 시도의 context 가 아닌 생성 context 를 잇는다.
     */
    public void process(String streamKey, MapRecord<String, String, String> record, int attempt) {
        Span span = telemetry.startProcessSpan(streamKey, record, attempt, streamKey.equals(props.getRetryKey()));
        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute(OUTCOME_ATTRIBUTE, handle(streamKey, record, attempt, span));
        } catch (RuntimeException e) {
            // retry/DLQ 인계 쓰기 실패. 원본은 ACK 없이 pending 에 남는다.
            telemetry.recordError(span, e);
            span.setAttribute(OUTCOME_ATTRIBUTE, "handoff_failed");
            throw e;
        } finally {
            span.end();
        }
    }

    private String handle(String streamKey, MapRecord<String, String, String> record, int attempt, Span span) {
        Map<String, String> body = record.getValue();
        SaveChatMessageCommand message;

        try {
            Long messageId = parseLong(body, ChatStreamField.MESSAGE_ID);
            Long roomId = parseLong(body, ChatStreamField.ROOM_ID);
            Long senderId = parseLong(body, ChatStreamField.SENDER_ID);
            String content = body.get(ChatStreamField.CONTENT);
            String messageTypeRaw = body.get(ChatStreamField.MESSAGE_TYPE);
            MessageType messageType = (messageTypeRaw != null && !messageTypeRaw.isBlank())
                    ? MessageType.valueOf(messageTypeRaw)
                    : MessageType.TEXT;
            if (messageType != MessageType.IMAGE && (content == null || content.isBlank())) {
                throw new InvalidChatStreamPayloadException();
            }
            List<Long> imageIds = parseImageIds(body.get(ChatStreamField.IMAGE_IDS));
            LocalDateTime createdAt = parseCreatedAt(body.get(ChatStreamField.CREATED_AT));
            message = new SaveChatMessageCommand(messageId, roomId, senderId, content, imageIds, messageType, createdAt);
        } catch (Exception e) {
            // 본문(content 등)과 예외 메시지(입력값 포함 가능)는 로그에 남기지 않는다.
            log.atError().addKeyValue("event", "chat_stream_invalid_payload")
                    .addKeyValue("record_id", record.getId().getValue())
                    .addKeyValue("error_type", e.getClass().getName())
                    .log("[ChatStream] 메시지 매핑 실패로 폐기합니다.");
            telemetry.recordError(span, e);
            telemetry.persistAttempt(ChatStreamTelemetry.PERSIST_INVALID_PAYLOAD);
            sendToDlq(streamKey, record, "MAPPING_ERROR: " + e.getMessage());
            ack(streamKey, record);
            return "invalid_payload";
        }

        // 저장(커밋) 성공 또는 retry/DLQ 인계 성공 뒤에만 ACK 한다. 인계 쓰기가 실패하면 예외가 전파되어 원본은 pending 에 남는다.
        long startedAt = System.nanoTime();
        try {
            saveChatMessageUseCase.execute(message);
        } catch (Exception e) {
            telemetry.persistDuration(System.nanoTime() - startedAt, ChatStreamTelemetry.OUTCOME_FAILURE);
            telemetry.recordError(span, e);
            boolean permanent = isPermanentFailure(e);
            telemetry.persistAttempt(permanent
                    ? ChatStreamTelemetry.PERSIST_PERMANENT_FAILURE
                    : ChatStreamTelemetry.PERSIST_TRANSIENT_FAILURE);
            String outcome;
            if (attempt >= props.getRetryMax() || permanent) {
                log.atError().addKeyValue("event", "chat_stream_dead_lettered")
                        .addKeyValue("record_id", record.getId().getValue())
                        .addKeyValue("message_id", message.messageId())
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("error_type", e.getClass().getName())
                        .setCause(e) // JSON formatter 는 예외 메시지를 빼고 타입·frame 만 남긴다
                        .log("[ChatStream] 메시지 처리 실패로 폐기합니다.");
                sendToDlq(streamKey, record, e.getMessage());
                outcome = "dead_lettered";
            } else {
                log.atWarn().addKeyValue("event", "chat_stream_retry_scheduled")
                        .addKeyValue("record_id", record.getId().getValue())
                        .addKeyValue("message_id", message.messageId())
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("error_type", e.getClass().getName())
                        .log("[ChatStream] 메시지 처리 실패, 재시도합니다.");
                sendToRetry(streamKey, record, attempt + 1, e.getMessage());
                outcome = "retry_scheduled";
            }
            ack(streamKey, record);
            return outcome;
        }
        telemetry.persistDuration(System.nanoTime() - startedAt, ChatStreamTelemetry.OUTCOME_SUCCESS);
        telemetry.persistAttempt(ChatStreamTelemetry.OUTCOME_SUCCESS);
        telemetry.publishToCommit(Long.parseLong(body.get(ChatStreamField.CREATED_AT).trim()), System.currentTimeMillis());
        ack(streamKey, record);
        return "committed";
    }

    private void sendToRetry(String streamKey, MapRecord<String, String, String> record, int attempt, String error) {
        try {
            redisAdapter.sendToRetry(streamKey, record, attempt, error);
        } catch (RuntimeException e) {
            telemetry.retry(false);
            throw e;
        }
        telemetry.retry(true);
    }

    private void sendToDlq(String streamKey, MapRecord<String, String, String> record, String error) {
        try {
            redisAdapter.sendToDlq(streamKey, record, error);
        } catch (RuntimeException e) {
            telemetry.dlq(false);
            throw e;
        }
        telemetry.dlq(true);
    }

    private void ack(String streamKey, MapRecord<String, String, String> record) {
        telemetry.ack(redisAdapter.ack(streamKey, record.getId()));
    }

    /**
     * 재시도해도 결과가 달라지지 않는 실패인지. 없는 방/회원, 차단 관계는 나중에 다시
     * 시도해도 그대로 실패하므로 retry 횟수만 소모하지 말고 바로 DLQ 로 보낸다.
     */
    private boolean isPermanentFailure(Exception e) {
        return e instanceof NotFoundChatRoomException
                || e instanceof ChatMessageIdConflictException
                || e instanceof NotFoundChatSenderException
                || e instanceof BlockedMemberException;
    }

    private List<Long> parseImageIds(String raw) {
        if (raw == null || raw.isBlank() || raw.equals("[]")) return null;
        try {
            return objectMapper.readValue(raw, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            throw new InvalidChatStreamPayloadException();
        }
    }

    private LocalDateTime parseCreatedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidChatStreamPayloadException();
        }
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(raw.trim())), ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            throw new InvalidChatStreamPayloadException();
        }
    }

    private Long parseLong(Map<String, String> body, String key) {
        String val = body.get(key);
        if (val == null || val.isBlank()) {
            throw new InvalidChatStreamPayloadException();
        }
        return Long.parseLong(val.trim());
    }
}
