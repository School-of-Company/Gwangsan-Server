package team.startup.gwangsan.adapter.web.chat.dto;

import java.time.LocalDateTime;

public record UpdateChatMessageResponse(Long messageId, Long roomId, String content, LocalDateTime editedAt) {
}
