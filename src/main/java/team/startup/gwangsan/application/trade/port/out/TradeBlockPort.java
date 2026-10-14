package team.startup.gwangsan.application.trade.port.out;

public interface TradeBlockPort {
    void validateNotBlocked(long memberId, long otherMemberId);
}
