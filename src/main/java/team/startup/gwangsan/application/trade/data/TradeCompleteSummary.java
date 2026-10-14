package team.startup.gwangsan.application.trade.data;

/** 거래 완료 요청/결과 요약(알림 원천 등). */
public record TradeCompleteSummary(long tradeId, long productId, String productTitle,
                                   long buyerId, String buyerNickname, long sellerId) {
}
