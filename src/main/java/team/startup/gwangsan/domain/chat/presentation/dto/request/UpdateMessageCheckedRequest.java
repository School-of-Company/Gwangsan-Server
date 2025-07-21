package team.startup.gwangsan.domain.chat.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateMessageCheckedRequest(
        @NotNull Long roomId,
        @NotNull Long lastMessageId
) {
}
