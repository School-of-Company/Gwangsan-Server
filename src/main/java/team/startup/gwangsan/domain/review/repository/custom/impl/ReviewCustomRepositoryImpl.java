package team.startup.gwangsan.domain.review.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.review.repository.custom.MyReviewRow;
import team.startup.gwangsan.domain.review.repository.custom.ReceivedReviewRow;
import team.startup.gwangsan.domain.review.repository.custom.ReviewCustomRepository;

import java.util.List;

import static team.startup.gwangsan.domain.review.entity.QReview.review;

@Repository
@RequiredArgsConstructor
public class ReviewCustomRepositoryImpl implements ReviewCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MyReviewRow> findMyReviews(Long reviewerId) {
        return queryFactory
                .select(com.querydsl.core.types.Projections.constructor(
                        MyReviewRow.class,
                        review.id,
                        review.product.id,
                        review.content,
                        review.light
                ))
                .from(review)
                .where(review.reviewer.id.eq(reviewerId))
                .orderBy(review.id.desc())
                .fetch();
    }

    @Override
    public List<ReceivedReviewRow> findReceivedReviews(Long reviewedId) {
        return queryFactory
                .select(com.querydsl.core.types.Projections.constructor(
                        ReceivedReviewRow.class,
                        review.id,
                        review.product.id,
                        review.content,
                        review.light,
                        review.reviewer.nickname
                ))
                .from(review)
                .where(review.reviewed.id.eq(reviewedId))
                .orderBy(review.id.desc())
                .fetch();
    }
}
