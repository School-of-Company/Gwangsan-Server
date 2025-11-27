package team.startup.gwangsan.domain.review.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.repository.custom.ReviewCustomRepository;

import java.util.List;

import static team.startup.gwangsan.domain.review.entity.QReview.review;
import static team.startup.gwangsan.domain.post.entity.QProduct.product;
import static team.startup.gwangsan.domain.member.entity.QMember.member;

@Repository
@RequiredArgsConstructor
public class ReviewCustomRepositoryImpl implements ReviewCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Review> findAllByReviewedWithFetch(Member reviewedMember) {
        return queryFactory
                .selectFrom(review)
                .join(review.product, product).fetchJoin()
                .join(review.reviewer, member).fetchJoin()
                .where(review.reviewed.eq(reviewedMember))
                .fetch();
    }
}
