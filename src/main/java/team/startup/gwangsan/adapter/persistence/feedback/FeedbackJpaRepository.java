package team.startup.gwangsan.adapter.persistence.feedback;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

public interface FeedbackJpaRepository extends JpaRepository<FeedbackJpaEntity, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FeedbackJpaEntity f set f.member = :dummy where f.member = :member")
    void reassignAuthor(@Param("member") MemberJpaEntity member, @Param("dummy") MemberJpaEntity dummy);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FeedbackJpaEntity f set f.answeredBy = :dummy where f.answeredBy = :member")
    void reassignAdmin(@Param("member") MemberJpaEntity member, @Param("dummy") MemberJpaEntity dummy);
}
