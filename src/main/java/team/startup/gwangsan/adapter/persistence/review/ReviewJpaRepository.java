package team.startup.gwangsan.adapter.persistence.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;

import java.util.List;

public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, Long>, ReviewCustomRepository {
    boolean existsByProductAndReviewer(Product product, MemberJpaEntity reviewer);

    long countByReviewed(MemberJpaEntity reviewed);

    List<ReviewJpaEntity> findAllByReviewer(MemberJpaEntity reviewer);

    List<ReviewJpaEntity> findAllByReviewed(MemberJpaEntity reviewed);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ReviewJpaEntity r SET r.reviewer = :dummy WHERE r.reviewer = :target")
    void reassignReviewer(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ReviewJpaEntity r SET r.reviewed = :dummy WHERE r.reviewed = :target")
    void reassignReviewed(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}