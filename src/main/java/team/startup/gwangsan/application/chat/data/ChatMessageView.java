package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageView(
        Long messageId,
        Long roomId,
        String content,
        MessageType messageType,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        List<ChatImage> images,
        String senderNickname,
        Long senderId,
        boolean checked,
        boolean isMine
) {
}
