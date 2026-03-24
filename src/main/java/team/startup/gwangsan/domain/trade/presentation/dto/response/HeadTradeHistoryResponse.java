package team.startup.gwangsan.domain.trade.presentation.dto.response;

import team.startup.gwangsan.domain.place.presentation.dto.PlaceDto;

public record HeadTradeHistoryResponse(
        PlaceDto place,
        Long tradeCount
) {
}
