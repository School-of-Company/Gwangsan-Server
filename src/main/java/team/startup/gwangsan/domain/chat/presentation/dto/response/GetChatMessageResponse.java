package team.startup.gwangsan.domain.chat.presentation.dto.response;

import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.time.LocalDateTime;
import java.util.List;

public record GetChatMessageResponse(
        Long messageId,
        Long roomId,
        String content,
        MessageType messageType,
        LocalDateTime createdAt,
        List<GetImageResponse> images,
        String senderNickname,
        Long senderId,
        boolean checked,
        boolean isMine
) {
}
