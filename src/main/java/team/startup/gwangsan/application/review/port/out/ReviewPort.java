package team.startup.gwangsan.application.review.port.out;

import team.startup.gwangsan.application.review.data.ReviewDetail;
import team.startup.gwangsan.application.review.data.ReviewPage;
import team.startup.gwangsan.application.review.data.ReviewRow;
import team.startup.gwangsan.domain.review.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewPort {
    boolean exists(long productId, long reviewerId);

    long save(Review review);

    /** bulk 재지정, 실행 뒤 영속성 컨텍스트를 비운다. */
    void reassignReviewer(Long memberId, Long dummyMemberId);

    void reassignReviewed(Long memberId, Long dummyMemberId);

    /** id 내림차순. */
    List<ReviewRow> findWritten(long reviewerId);

    /** id 내림차순, cursor 미만. */
    List<ReviewRow> findReceived(long reviewedId, ReviewPage page);

    /** images 는 비어 있는 상태로 돌려준다. */
    Optional<ReviewDetail> findDetail(long reviewId);
}
