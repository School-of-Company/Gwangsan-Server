package team.startup.gwangsan.adapter.web.trade.dto;

import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;

public record GetTradeProductResponse(
        Long productId,
        String title,
        Integer gwangsan,
        GetImageResponse image
) {
}
