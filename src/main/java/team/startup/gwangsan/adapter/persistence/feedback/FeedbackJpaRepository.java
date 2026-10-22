package team.startup.gwangsan.adapter.persistence.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

public interface FeedbackJpaRepository extends JpaRepository<FeedbackJpaEntity, Long>, FeedbackCustomRepository {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE FeedbackJpaEntity f SET f.member = :dummy WHERE f.member = :target")
    void reassignMember(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE FeedbackJpaEntity f SET f.answeredBy = :dummy WHERE f.answeredBy = :target")
    void reassignAnsweredBy(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
