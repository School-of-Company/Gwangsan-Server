package team.startup.gwangsan.application.trade.port.out;

/** 회원·잔액(member 소유). 잔액 변경은 호출자 트랜잭션에 참여한다. */
public interface TradeMemberPort {
    void requireMember(long memberId);

    void transfer(long fromMemberId, long toMemberId, int gwangsan);
}
