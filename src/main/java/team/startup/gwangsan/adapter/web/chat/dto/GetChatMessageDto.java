package team.startup.gwangsan.adapter.web.chat.dto;

import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.time.LocalDateTime;
import java.util.List;

public record GetChatMessageDto(
        Long messageId,
        Long roomId,
        String content,
        MessageType messageType,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        List<GetImageResponse> images,
        String senderNickname,
        Long senderId,
        boolean checked,
        boolean isMine
) {
}
