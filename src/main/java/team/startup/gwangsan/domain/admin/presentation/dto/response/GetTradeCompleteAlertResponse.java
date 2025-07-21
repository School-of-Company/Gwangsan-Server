package team.startup.gwangsan.domain.admin.presentation.dto.response;

import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;

import java.time.LocalDateTime;

public record GetTradeCompleteAlertResponse(
        Long id,
        String nickname,
        String title,
        LocalDateTime createdAt,
        GetProductResponse product
) {
}
