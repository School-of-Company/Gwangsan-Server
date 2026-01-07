package team.startup.gwangsan.domain.review.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.review.repository.projection.MyReviewDto;
import team.startup.gwangsan.domain.review.repository.projection.ReceivedReviewDto;
import team.startup.gwangsan.domain.review.repository.custom.ReviewCustomRepository;
import com.querydsl.core.types.Projections;

import java.util.List;

import static team.startup.gwangsan.domain.review.entity.QReview.review;

@Repository
@RequiredArgsConstructor
public class ReviewCustomRepositoryImpl implements ReviewCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MyReviewDto> findMyReviews(Long reviewerId) {
        return queryFactory
                .select(Projections.constructor(
                        MyReviewDto.class,
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
    public List<ReceivedReviewDto> findReceivedReviews(Long reviewedId) {
        return queryFactory
                .select(Projections.constructor(
                        ReceivedReviewDto.class,
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
