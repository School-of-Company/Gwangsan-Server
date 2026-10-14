package team.startup.gwangsan.adapter.web.notice.dto;

import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;

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

