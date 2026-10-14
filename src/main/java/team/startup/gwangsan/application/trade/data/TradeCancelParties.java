package team.startup.gwangsan.application.trade.data;

/** 철회 요청자와 거래 당사자. */
public record TradeCancelParties(long tradeCancelId, long requesterId, long buyerId, long sellerId) {
}
