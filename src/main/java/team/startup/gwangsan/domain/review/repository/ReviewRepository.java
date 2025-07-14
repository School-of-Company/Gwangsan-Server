package team.startup.gwangsan.domain.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.review.entity.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByProductAndReviewer(Product product, Member reviewer);

    List<Review> findAllByReviewer(Member reviewer);

    List<Review> findAllByReviewed(Member reviewed);
}