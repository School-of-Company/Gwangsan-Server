package team.startup.gwangsan.domain.admin.presentation.dto.response;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;

import java.time.LocalDateTime;
import java.util.List;

public record GetTradeCancelAlertResponse(
        Long id,
        String nickname,
        String title,
        String reason,
        String placeName,
        LocalDateTime createdAt,
        List<GetImageResponse> images,
        GetProductResponse product
) {
}
