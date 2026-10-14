package team.startup.gwangsan.application.trade.data;

/** 거래 완료 요청/확정/요청 취소. actorId 는 인증 principal 에서 온 실행자다. */
public record TradeCompletionCommand(long actorId, long productId, long otherMemberId) {
}
