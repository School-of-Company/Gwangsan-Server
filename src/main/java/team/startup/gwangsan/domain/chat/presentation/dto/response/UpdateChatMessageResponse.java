package team.startup.gwangsan.domain.chat.presentation.dto.response;

import java.time.LocalDateTime;

public record UpdateChatMessageResponse(Long messageId, Long roomId, String content, LocalDateTime editedAt) {
}
