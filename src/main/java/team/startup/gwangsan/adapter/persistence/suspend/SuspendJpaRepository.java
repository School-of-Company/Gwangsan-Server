package team.startup.gwangsan.adapter.persistence.suspend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface SuspendJpaRepository extends JpaRepository<SuspendJpaEntity, Long> {

    /**
     * 해제 대상 회원 ID. 만료 정지가 있고, 현재 SUSPENDED 이며, 진행 중(suspendedUntil >= now) 정지가 없는 회원만 한 번씩.
     * 정지 이력은 지우지 않으므로 상태·진행 중 정지 조건이 재정지 조기 해제와 다른 상태 덮어쓰기를 막는다
     * (fix/suspension-release-active-period 의 findReleasableMembers 와 같은 조건).
     */
    @Query("""
            SELECT DISTINCT s.member.id FROM SuspendJpaEntity s
            WHERE s.suspendedUntil < :now
              AND s.member.status = team.startup.gwangsan.domain.member.model.MemberStatus.SUSPENDED
              AND NOT EXISTS (
                  SELECT 1 FROM SuspendJpaEntity active
                  WHERE active.member = s.member AND active.suspendedUntil >= :now
              )
            """)
    List<Long> findReleasableMemberIds(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SuspendJpaEntity s WHERE s.member = :member")
    void deleteAllByMember(@Param("member") MemberJpaEntity member);
}
