package team.startup.gwangsan.domain.chat.presentation.dto.response;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.time.LocalDateTime;
import java.util.List;

public record SaveChatMessageResponse(
        Long messageId,
        List<GetImageResponse> images,
        LocalDateTime createdAt,
        Long senderId,
        boolean checked
) {
}
