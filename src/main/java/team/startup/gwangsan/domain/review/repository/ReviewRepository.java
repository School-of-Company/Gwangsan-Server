package team.startup.gwangsan.domain.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.review.entity.Review;
import team.startup.gwangsan.domain.review.repository.custom.ReviewCustomRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewCustomRepository {
    boolean existsByProductAndReviewer(Product product, MemberJpaEntity reviewer);

    long countByReviewed(MemberJpaEntity reviewed);

    List<Review> findAllByReviewer(MemberJpaEntity reviewer);

    List<Review> findAllByReviewed(MemberJpaEntity reviewed);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.reviewer = :dummy WHERE r.reviewer = :target")
    void reassignReviewer(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.reviewed = :dummy WHERE r.reviewed = :target")
    void reassignReviewed(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}