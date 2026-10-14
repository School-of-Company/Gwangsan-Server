package team.startup.gwangsan.adapter.web.trade.dto;

public record HeadTradeHistoryResponse(
        TradePlaceResponse place,
        Long tradeCount
) {
}
