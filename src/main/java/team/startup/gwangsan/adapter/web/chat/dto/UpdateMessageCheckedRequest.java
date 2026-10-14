package team.startup.gwangsan.adapter.web.chat.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMessageCheckedRequest(
        @NotNull Long roomId,
        @NotNull Long lastMessageId
) {
}
