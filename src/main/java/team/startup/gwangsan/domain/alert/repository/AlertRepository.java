package team.startup.gwangsan.domain.alert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.member.entity.Member;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Alert a SET a.sendMember = :dummy WHERE a.sendMember = :target")
    void reassignSendMember(@Param("target") Member target, @Param("dummy") Member dummy);
}
