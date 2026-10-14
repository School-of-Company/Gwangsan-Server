package team.startup.gwangsan.domain.trade.model;

import team.startup.gwangsan.domain.trade.exception.AlreadyTradeCancelRequestException;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;
import team.startup.gwangsan.domain.trade.exception.NotTradeCancelRequesterException;

/**
 * 완료된 거래의 철회 요청.
 */
public final class TradeCancel {

    private final Long id;
    private final long tradeCompleteId;
    private final long requesterId;
    private final String reason;
    private TradeCancelStatus status;

    public TradeCancel(Long id, long tradeCompleteId, long requesterId, String reason, TradeCancelStatus status) {
        this.id = id;
        this.tradeCompleteId = tradeCompleteId;
        this.requesterId = requesterId;
        this.reason = reason;
        this.status = status;
    }

    public static TradeCancel request(long tradeCompleteId, long requesterId, String reason) {
        return new TradeCancel(null, tradeCompleteId, requesterId, reason, TradeCancelStatus.PENDING);
    }

    /** 상대방의 대기 요청에 같은 요청을 내면 양측 동의다. 본인이 이미 낸 요청이면 거절한다. */
    public void validateAgreementBy(long memberId) {
        if (requesterId == memberId) {
            throw new AlreadyTradeCancelRequestException();
        }
    }

    public void approve() {
        if (status != TradeCancelStatus.PENDING) {
            throw new CannotPendingTradeCancelException();
        }
        this.status = TradeCancelStatus.APPROVED;
    }

    /** 관리자 기각. PENDING 이 아니면 거절한다(이미 승인·철회·기각된 요청을 덮어쓰지 않는다). */
    public void reject() {
        if (status != TradeCancelStatus.PENDING) {
            throw new CannotPendingTradeCancelException();
        }
        this.status = TradeCancelStatus.REJECTED;
    }

    public void withdraw(long memberId) {
        if (requesterId != memberId) {
            throw new NotTradeCancelRequesterException();
        }
        this.status = TradeCancelStatus.WITHDRAWN;
    }

    public Long id() { return id; }
    public long tradeCompleteId() { return tradeCompleteId; }
    public long requesterId() { return requesterId; }
    public String reason() { return reason; }
    public TradeCancelStatus status() { return status; }
}
