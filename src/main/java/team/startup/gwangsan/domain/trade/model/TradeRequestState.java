package team.startup.gwangsan.domain.trade.model;

import java.time.LocalDateTime;

/** 한 채팅방(상품+구매자+판매자)의 PENDING/COMPLETED 요청 한 건의 상태 값. */
public record TradeRequestState(TradeStatus status, Boolean requestedBySeller, LocalDateTime createdAt) {
}
