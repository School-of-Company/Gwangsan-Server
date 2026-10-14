package team.startup.gwangsan.adapter.persistence.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

public interface AlertReceiptJpaRepository extends JpaRepository<AlertReceiptJpaEntity, Long> {

    boolean existsByMemberIdAndChecked(Long memberId, boolean check);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AlertReceiptJpaEntity r WHERE r.member = :member")
    void deleteAllByMember(@Param("member") MemberJpaEntity member);
}
