package team.startup.gwangsan.domain.chat.model;

import java.time.LocalDateTime;

/**
 * 채팅방 참여자와 참여자별 숨김 상태. 숨김은 목록에서만 빠지고 방·메시지는 유지된다.
 */
public record ChatRoom(
        Long id,
        Long productId,
        Long buyerId,
        Long sellerId,
        LocalDateTime hiddenByBuyerAt,
        LocalDateTime hiddenBySellerAt
) {

    public boolean isParticipant(Long memberId) {
        return buyerId.equals(memberId) || sellerId.equals(memberId);
    }

    public boolean isSeller(Long memberId) {
        return sellerId.equals(memberId);
    }

    /** 참여자가 아니면 구매자를 돌려준다 (기존 Entity 동작 유지). */
    public Long otherMemberId(Long memberId) {
        return buyerId.equals(memberId) ? sellerId : buyerId;
    }

    public boolean isHiddenFor(Long memberId) {
        if (buyerId.equals(memberId)) {
            return hiddenByBuyerAt != null;
        }
        if (sellerId.equals(memberId)) {
            return hiddenBySellerAt != null;
        }
        return false;
    }

    public ChatRoom hideFor(Long memberId, LocalDateTime hiddenAt) {
        return withHidden(memberId, hiddenAt);
    }

    public ChatRoom unhideFor(Long memberId) {
        return withHidden(memberId, null);
    }

    private ChatRoom withHidden(Long memberId, LocalDateTime hiddenAt) {
        if (buyerId.equals(memberId)) {
            return new ChatRoom(id, productId, buyerId, sellerId, hiddenAt, hiddenBySellerAt);
        }
        if (sellerId.equals(memberId)) {
            return new ChatRoom(id, productId, buyerId, sellerId, hiddenByBuyerAt, hiddenAt);
        }
        return this;
    }
}
