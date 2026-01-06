package team.startup.gwangsan.domain.review.repository.custom;

import java.util.List;

public interface ReviewCustomRepository {
    List<MyReviewRow> findMyReviews(Long reviewerId);
    List<ReceivedReviewRow> findReceivedReviews(Long reviewedId);

}
