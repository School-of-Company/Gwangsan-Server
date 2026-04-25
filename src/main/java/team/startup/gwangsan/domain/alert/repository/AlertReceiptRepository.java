package team.startup.gwangsan.domain.alert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.alert.entity.AlertReceipt;
import team.startup.gwangsan.domain.alert.repository.custom.AlertReceiptCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;

public interface AlertReceiptRepository extends JpaRepository<AlertReceipt, Long>, AlertReceiptCustomRepository {
    boolean existsByMemberIdAndChecked(Long memberId, boolean check);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AlertReceipt r WHERE r.member = :member")
    void deleteAllByMember(@Param("member") Member member);
}
