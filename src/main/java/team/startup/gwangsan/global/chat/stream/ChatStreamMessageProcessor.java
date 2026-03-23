package team.startup.gwangsan.global.chat.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.InvalidChatStreamPayloadException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

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
            redisAdapter.sendToDlq(streamKey, record, "MAPPING_ERROR: " + e.getMessage());
            redisAdapter.ack(streamKey, record.getId());
            return;
        }

        for (ChatStreamHandler handler : handlers) {
            try {
                handler.handle(message);
            } catch (Exception e) {
                if (attempt >= props.getRetryMax()) {
                    redisAdapter.sendToDlq(streamKey, record, e.getMessage());
                } else {
                    redisAdapter.sendToRetry(streamKey, record, attempt + 1, e.getMessage());
                }
                redisAdapter.ack(streamKey, record.getId());
                return;
            }
        }

        redisAdapter.ack(streamKey, record.getId());
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
