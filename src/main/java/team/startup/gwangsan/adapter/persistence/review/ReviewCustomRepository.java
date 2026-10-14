package team.startup.gwangsan.adapter.persistence.review;


import java.util.List;

public interface ReviewCustomRepository {
    List<MyReviewDto> findMyReviews(Long reviewerId);
    List<ReceivedReviewDto> findReceivedReviews(Long reviewedId);
    List<ReceivedReviewDto> findReceivedReviews(Long reviewedId, Long cursor, Integer size);

}
