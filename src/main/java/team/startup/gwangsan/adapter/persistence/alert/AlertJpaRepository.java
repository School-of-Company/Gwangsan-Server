package team.startup.gwangsan.adapter.persistence.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

public interface AlertJpaRepository extends JpaRepository<AlertJpaEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AlertJpaEntity a SET a.sendMember = :dummy WHERE a.sendMember = :target")
    void reassignSendMember(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
