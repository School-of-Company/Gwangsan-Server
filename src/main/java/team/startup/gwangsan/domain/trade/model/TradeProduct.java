package team.startup.gwangsan.domain.trade.model;

import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteException;

/**
 * 거래 판단에 필요한 상품 사실. ownerIsSeller 는 작성자가 판매자 쪽인지(GIVER 글)를 뜻한다.
 */
public record TradeProduct(long productId, long ownerId, boolean ownerIsSeller, ProductPhase phase, int gwangsan) {

    public void validateNotCompleted() {
        if (phase == ProductPhase.COMPLETED) {
            throw new TradeAlreadyCompleteException();
        }
    }

    /** 게시글 작성자는 상대 확인 없이 거래를 즉시 완료할 수 있다. */
    public boolean isOwnedBy(long memberId) {
        return ownerId == memberId;
    }

    public boolean reserved() {
        return phase == ProductPhase.RESERVED;
    }

    /** GIVER 글은 작성자가 아닌 쪽이, RECEIVER 글은 작성자가 구매자다. */
    public TradeSides sides(long actorId, long otherMemberId) {
        boolean actorIsBuyer = ownerIsSeller ? actorId != ownerId : actorId == ownerId;
        return actorIsBuyer
                ? new TradeSides(actorId, otherMemberId, true)
                : new TradeSides(otherMemberId, actorId, false);
    }
}
