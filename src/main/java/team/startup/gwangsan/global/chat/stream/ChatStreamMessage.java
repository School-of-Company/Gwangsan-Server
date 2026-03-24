package team.startup.gwangsan.global.chat.stream;

import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import java.time.LocalDateTime;
import java.util.List;

public record ChatStreamMessage(
        Long messageId,
        Long roomId,
        String content,
        List<Long> imageIds,
        MessageType messageType,
        Long senderId,
        LocalDateTime createdAt
) {}
