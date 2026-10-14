package team.startup.gwangsan.adapter.web.notice.dto;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.time.LocalDateTime;
import java.util.List;

public record FindNoticeResponse(
        Long id,
        String title,
        String content,
        String place,
        LocalDateTime createdAt,
        String role,
        List<GetImageResponse> images,
        boolean isMe
) {
}

