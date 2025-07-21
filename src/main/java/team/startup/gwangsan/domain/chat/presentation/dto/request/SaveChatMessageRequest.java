package team.startup.gwangsan.domain.chat.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;

import java.util.List;

public record SaveChatMessageRequest(
        @NotNull Long roomId,
        @NotNull String content,
        List<Long> imageIds,
        @NotNull MessageType messageType
) {
}
