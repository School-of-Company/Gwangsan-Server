package team.startup.gwangsan.domain.review.model;

import team.startup.gwangsan.domain.review.exception.CannotReviewSelfException;
import team.startup.gwangsan.domain.review.exception.NotTradeParticipantException;

/**
 * 완료된 거래 상대방에 대한 후기.
 */
public record Review(Long id, long productId, long reviewerId, long reviewedId, String content, Integer light) {

    public static void validateNotSelf(long reviewerId, long reviewedId) {
        if (reviewerId == reviewedId) {
            throw new CannotReviewSelfException();
        }
    }

    /** 후기 작성자와 대상이 완료된 거래의 구매자·판매자 한 쌍이어야 한다. */
    public static void validateParticipants(long reviewerId, long reviewedId, long buyerId, long sellerId) {
        boolean participants = (buyerId == reviewerId && sellerId == reviewedId)
                || (sellerId == reviewerId && buyerId == reviewedId);
        if (!participants) {
            throw new NotTradeParticipantException();
        }
    }
}
