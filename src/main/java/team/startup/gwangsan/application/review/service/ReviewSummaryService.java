package team.startup.gwangsan.application.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.review.data.ReviewAuthor;
import team.startup.gwangsan.application.review.port.in.ReviewSummaryQuery;
import team.startup.gwangsan.application.review.port.out.ReviewPort;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;

/** 조회 전용. 호출자 트랜잭션이 있으면 참여하고, 없으면 지연 로딩을 위해 읽기 트랜잭션을 연다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewSummaryService implements ReviewSummaryQuery {

    private final ReviewPort reviewPort;

    @Override
    public ReviewAuthor getAuthor(long reviewId) {
        return reviewPort.findAuthor(reviewId).orElseThrow(NotFoundReviewException::new);
    }
}
