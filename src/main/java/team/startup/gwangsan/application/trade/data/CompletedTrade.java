package team.startup.gwangsan.application.trade.data;

import java.time.LocalDateTime;

public record CompletedTrade(long tradeId, long buyerId, long sellerId, LocalDateTime completedAt) {
}
