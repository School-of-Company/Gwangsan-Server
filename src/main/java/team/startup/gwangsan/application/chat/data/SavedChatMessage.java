package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.chat.model.ChatImage;

import java.time.LocalDateTime;
import java.util.List;

public record SavedChatMessage(
        Long messageId,
        List<ChatImage> images,
        LocalDateTime createdAt,
        Long senderId,
        boolean checked
) {
}
