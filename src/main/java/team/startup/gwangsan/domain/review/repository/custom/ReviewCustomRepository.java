package team.startup.gwangsan.domain.review.repository.custom;

import team.startup.gwangsan.domain.review.repository.projection.MyReviewDto;
import team.startup.gwangsan.domain.review.repository.projection.ReceivedReviewDto;

import java.util.List;

public interface ReviewCustomRepository {
    List<MyReviewDto> findMyReviews(Long reviewerId);
    List<ReceivedReviewDto> findReceivedReviews(Long reviewedId);

}
