package team.startup.gwangsan.domain.admin.presentation.dto.response;

import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;

import java.time.LocalDateTime;

public record GetTradeCompleteAlertResponse(
        String nickname,
        String title,
        String placeName,
        LocalDateTime createdAt,
        GetProductResponse product
) {
}
