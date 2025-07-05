package team.startup.gwangsan.domain.notice.presentation.dto.response;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.util.List;

public record FindAllNoticeResponse(
        Long id,
        String title,
        String content,
        String place,
        String createdAt,
        String role,
        List<GetImageResponse> images
) {}
