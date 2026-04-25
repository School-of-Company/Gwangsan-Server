package team.startup.gwangsan.domain.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.repository.custom.ReviewCustomRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewCustomRepository {
    boolean existsByProductAndReviewer(Product product, Member reviewer);

    long countByReviewed(Member reviewed);

    List<Review> findAllByReviewer(Member reviewer);

    List<Review> findAllByReviewed(Member reviewed);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.reviewer = :dummy WHERE r.reviewer = :target")
    void reassignReviewer(@Param("target") Member target, @Param("dummy") Member dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.reviewed = :dummy WHERE r.reviewed = :target")
    void reassignReviewed(@Param("target") Member target, @Param("dummy") Member dummy);
}