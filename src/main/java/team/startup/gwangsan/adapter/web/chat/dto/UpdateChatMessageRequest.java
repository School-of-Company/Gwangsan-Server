package team.startup.gwangsan.adapter.web.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateChatMessageRequest(@NotBlank String content) {
}
