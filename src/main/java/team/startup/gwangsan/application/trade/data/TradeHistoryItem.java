package team.startup.gwangsan.application.trade.data;

import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.time.LocalDateTime;

/** image 는 대표 이미지가 없으면 null. */
public record TradeHistoryItem(long tradeId, boolean actorIsBuyer, TradeStatus status, LocalDateTime completedAt,
                               long otherMemberId, String otherNickname,
                               long productId, String productTitle, Integer gwangsan, TradeImage image) {
}
