package team.startup.gwangsan.application.trade.data;

import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.time.LocalDateTime;

public record TradeHistoryRow(long tradeId, long buyerId, String buyerNickname, long sellerId, String sellerNickname,
                              TradeStatus status, LocalDateTime completedAt,
                              long productId, String productTitle, Integer gwangsan) {
}
