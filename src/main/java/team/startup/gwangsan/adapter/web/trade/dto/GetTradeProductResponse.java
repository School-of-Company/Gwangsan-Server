package team.startup.gwangsan.adapter.web.trade.dto;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

public record GetTradeProductResponse(
        Long productId,
        String title,
        Integer gwangsan,
        GetImageResponse image
) {
}
