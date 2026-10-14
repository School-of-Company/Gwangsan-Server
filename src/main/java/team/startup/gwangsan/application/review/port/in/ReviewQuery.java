package team.startup.gwangsan.application.review.port.in;

import team.startup.gwangsan.application.review.data.ReviewDetail;
import team.startup.gwangsan.application.review.data.ReviewItem;
import team.startup.gwangsan.application.review.data.ReviewPage;

import java.util.List;

public interface ReviewQuery {
    List<ReviewItem> findWritten(long actorId);

    List<ReviewItem> findReceived(long actorId, ReviewPage page);

    /** 회원이 없으면 회원 소유자의 기존 오류. */
    List<ReviewItem> findByMember(long memberId, ReviewPage page);

    ReviewDetail findDetail(long reviewId);
}
