package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.chat.model.MessageType;

import java.time.LocalDateTime;

public record ChatMessageRow(
        Long messageId,
        Long roomId,
        String content,
        MessageType messageType,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        Long senderId,
        String senderNickname,
        boolean checked
) {
}
