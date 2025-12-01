package team.startup.gwangsan.domain.chat.repository.projection;

import team.startup.gwangsan.domain.chat.entity.constant.MessageType;

import java.time.LocalDateTime;

public record LatestMessageDto(
        Long roomId,
        Long messageId,
        String content,
        MessageType messageType,
        LocalDateTime createdAt
) {
}