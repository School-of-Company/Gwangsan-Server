package team.startup.gwangsan.domain.chat.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateChatMessageRequest(@NotBlank String content) {
}
