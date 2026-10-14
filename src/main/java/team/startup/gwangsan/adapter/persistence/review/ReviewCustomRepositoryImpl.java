package team.startup.gwangsan.adapter.persistence.review;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.querydsl.core.types.Projections;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class ReviewCustomRepositoryImpl implements ReviewCustomRepository {

    private static final QReviewJpaEntity review = QReviewJpaEntity.reviewJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MyReviewDto> findMyReviews(Long reviewerId) {
        return queryFactory
                .select(Projections.constructor(
                        MyReviewDto.class,
                        review.id,
                        review.product.id,
                        review.content,
                        review.light,
                        review.reviewed.nickname
                ))
                .from(review)
                .where(review.reviewer.id.eq(reviewerId))
                .orderBy(review.id.desc())
                .fetch();
    }

    @Override
    public List<ReceivedReviewDto> findReceivedReviews(Long reviewedId) {
        return findReceivedReviews(reviewedId, null, null);
    }

    @Override
    public List<ReceivedReviewDto> findReceivedReviews(Long reviewedId, Long cursor, Integer size) {
        var query = queryFactory
                .select(Projections.constructor(
                        ReceivedReviewDto.class,
                        review.id,
                        review.product.id,
                        review.content,
                        review.light,
                        review.reviewer.nickname
                ))
                .from(review)
                .where(review.reviewed.id.eq(reviewedId), cursor == null ? null : review.id.lt(cursor))
                .orderBy(review.id.desc());
        if (size != null) query.limit(size);
        return query.fetch();
    }
}
