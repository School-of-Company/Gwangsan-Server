package team.startup.gwangsan.domain.trade.presentation.dto.response;

import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.presentation.dto.response.constant.TradeRole;

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
