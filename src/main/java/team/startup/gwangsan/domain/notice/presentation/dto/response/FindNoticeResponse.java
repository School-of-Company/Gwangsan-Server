package team.startup.gwangsan.domain.notice.presentation.dto.response;

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
        List<GetImageResponse> images
) {
}

