package team.startup.gwangsan.application.trade.data;

import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;

/** 철회 요청 공개 조회 값. */
public record TradeCancelView(long tradeCancelId, long productId, long requesterId, String reason, TradeCancelStatus status) {
}
