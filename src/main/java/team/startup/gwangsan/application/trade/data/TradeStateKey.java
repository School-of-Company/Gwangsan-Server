package team.startup.gwangsan.application.trade.data;

/** 한 채팅방(상품 + 구매자 + 판매자). */
public record TradeStateKey(long productId, long buyerId, long sellerId) {
}
