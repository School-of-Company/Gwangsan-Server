package team.startup.gwangsan.domain.trade.presentation.dto.response;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

public record GetTradeProductResponse(
        Long productId,
        String title,
        Integer gwangsan,
        GetImageResponse image
) {
}
