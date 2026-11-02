package team.startup.gwangsan.application.trade.port.out;

/** 회원·잔액(member 소유). 잔액 변경은 호출자 트랜잭션에 참여한다. */
public interface TradeMemberPort {
    void requireMember(long memberId);

    /** 거래 완료 대금. 원장 사유 TRADE_COMPLETE, 기준 ID 는 완료 거래 ID 다. */
    void transferForCompletion(long fromMemberId, long toMemberId, int gwangsan, long tradeCompleteId);

    /** 거래 철회 환불. 원장 사유 TRADE_CANCEL, 기준 ID 는 철회 ID 다. */
    void transferForCancel(long fromMemberId, long toMemberId, int gwangsan, long tradeCancelId);
}
