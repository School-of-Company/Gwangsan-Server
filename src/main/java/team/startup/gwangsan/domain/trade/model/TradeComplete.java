package team.startup.gwangsan.domain.trade.model;

import team.startup.gwangsan.domain.trade.exception.NotTradeCompleteRequesterException;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteRequestException;

import java.time.LocalDateTime;

/**
 * 한 상품의 구매자·판매자 사이 거래 완료 요청과 그 결과.
 * id/createdAt 은 저장 후에 채워진다.
 */
public final class TradeComplete {

    private final Long id;
    private final long productId;
    private final long buyerId;
    private final long sellerId;
    private final boolean requestedBySeller;
    private final LocalDateTime createdAt;
    private TradeStatus status;
    private LocalDateTime completedAt;

    public TradeComplete(Long id, long productId, long buyerId, long sellerId, TradeStatus status,
                         boolean requestedBySeller, LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id = id;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.status = status;
        this.requestedBySeller = requestedBySeller;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static TradeComplete request(long productId, TradeSides sides) {
        return new TradeComplete(null, productId, sides.buyerId(), sides.sellerId(), TradeStatus.PENDING,
                !sides.actorIsBuyer(), null, null);
    }

    /** 게시글 작성자가 대기 요청 없이 호출하면 요청 단계 없이 완료된 거래를 만든다. */
    public static TradeComplete completeByAuthor(long productId, TradeSides sides, LocalDateTime now) {
        return new TradeComplete(null, productId, sides.buyerId(), sides.sellerId(), TradeStatus.COMPLETED,
                !sides.actorIsBuyer(), null, now);
    }

    /** 요청을 만든 쪽과 호출자의 역할이 같으면 본인 요청을 스스로 확정하려는 것이다. */
    public boolean isRequestedBy(boolean actorIsBuyer) {
        return actorIsBuyer != requestedBySeller;
    }

    public void confirm(boolean actorIsBuyer, LocalDateTime now) {
        if (isRequestedBy(actorIsBuyer)) {
            throw new TradeAlreadyCompleteRequestException();
        }
        this.status = TradeStatus.COMPLETED;
        this.completedAt = now;
    }

    /** 게시글 작성자는 대기 요청을 누가 만들었든 확정할 수 있다. */
    public void confirmByAuthor(LocalDateTime now) {
        this.status = TradeStatus.COMPLETED;
        this.completedAt = now;
    }

    public void validateWithdrawableBy(boolean actorIsBuyer) {
        if (!isRequestedBy(actorIsBuyer)) {
            throw new NotTradeCompleteRequesterException();
        }
    }

    public boolean isParticipant(long memberId) {
        return buyerId == memberId || sellerId == memberId;
    }

    public void rollBack() {
        this.status = TradeStatus.ROLLED_BACK;
    }

    public Long id() { return id; }
    public long productId() { return productId; }
    public long buyerId() { return buyerId; }
    public long sellerId() { return sellerId; }
    public TradeStatus status() { return status; }
    public boolean requestedBySeller() { return requestedBySeller; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime completedAt() { return completedAt; }
}
