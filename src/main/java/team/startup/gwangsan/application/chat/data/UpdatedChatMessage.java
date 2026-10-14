package team.startup.gwangsan.application.chat.data;

import java.time.LocalDateTime;

public record UpdatedChatMessage(Long messageId, Long roomId, String content, LocalDateTime editedAt) {
}
