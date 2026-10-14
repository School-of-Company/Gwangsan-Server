package team.startup.gwangsan.adapter.web.trade.dto;

import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.time.LocalDateTime;

public record GetTradeHistoryResponse(
        Long tradeId,
        TradeRole role,
        TradeStatus status,
        LocalDateTime completedAt,
        GetTradeMemberResponse otherMember,
        GetTradeProductResponse product
) {
}
