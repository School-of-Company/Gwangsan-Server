package team.startup.gwangsan.adapter.persistence.chat;

import team.startup.gwangsan.domain.chat.model.MessageType;

import java.time.LocalDateTime;

public record LatestMessageDto(
        Long roomId,
        Long messageId,
        String content,
        MessageType messageType,
        LocalDateTime createdAt
) {
}