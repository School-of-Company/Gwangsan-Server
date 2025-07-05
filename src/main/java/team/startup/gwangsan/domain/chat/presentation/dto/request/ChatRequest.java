package team.startup.gwangsan.domain.chat.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatRequest(
        @NotNull Long productId
        ) {
}
