package team.startup.gwangsan.domain.review.repository.custom;

import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.review.entity.Review;

import java.util.List;

public interface ReviewCustomRepository {
    List<Review> findAllByReviewedWithFetch(Member reviewed);
}
