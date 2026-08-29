package team.startup.gwangsan.global.chat.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.InvalidChatStreamPayloadException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

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

    private final List<ChatStreamHandler> handlers;
    private final ChatStreamRedisAdapter redisAdapter;
    private final ChatStreamProperties props;
    private final Gson gson;

    public void process(String streamKey, MapRecord<String, String, String> record, int attempt) {
        Map<String, String> body = record.getValue();
        ChatStreamMessage message;

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
            message = new ChatStreamMessage(messageId, roomId, content, imageIds, messageType, senderId, createdAt);
        } catch (Exception e) {
            log.error("[ChatStream] 메시지 매핑 실패로 폐기합니다. streamKey={}, recordId={}, body={}",
                    streamKey, record.getId(), body, e);
            redisAdapter.sendToDlq(streamKey, record, "MAPPING_ERROR: " + e.getMessage());
            redisAdapter.ack(streamKey, record.getId());
            return;
        }

        for (ChatStreamHandler handler : handlers) {
            try {
                handler.handle(message);
            } catch (Exception e) {
                if (attempt >= props.getRetryMax() || isPermanentFailure(e)) {
                    log.error("[ChatStream] 메시지 처리 실패로 폐기합니다. streamKey={}, recordId={}, messageId={}, attempt={}",
                            streamKey, record.getId(), message.messageId(), attempt, e);
                    redisAdapter.sendToDlq(streamKey, record, e.getMessage());
                } else {
                    log.warn("[ChatStream] 메시지 처리 실패, 재시도합니다. streamKey={}, messageId={}, attempt={}, cause={}",
                            streamKey, message.messageId(), attempt, e.getMessage());
                    redisAdapter.sendToRetry(streamKey, record, attempt + 1, e.getMessage());
                }
                redisAdapter.ack(streamKey, record.getId());
                return;
            }
        }

        redisAdapter.ack(streamKey, record.getId());
    }

    /**
     * 재시도해도 결과가 달라지지 않는 실패인지. 없는 방/회원은 나중에 다시 시도해도
     * 그대로 실패하므로 retry 횟수만 소모하지 말고 바로 DLQ 로 보낸다.
     */
    private boolean isPermanentFailure(Exception e) {
        return e instanceof NotFoundChatRoomException || e instanceof NotFoundMemberException;
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
