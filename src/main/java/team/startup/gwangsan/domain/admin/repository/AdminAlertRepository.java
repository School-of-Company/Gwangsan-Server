package team.startup.gwangsan.domain.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.repository.custom.AdminAlertCustomRepository;
import team.startup.gwangsan.domain.member.entity.Member;

import java.util.Optional;

public interface AdminAlertRepository extends JpaRepository<AdminAlert, Long>, AdminAlertCustomRepository {
    Optional<AdminAlert> findBySourceId(Long sourceId);

    Optional<AdminAlert> findByIdAndType(Long alertId, AlertType type);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AdminAlert a SET a.otherMember = :dummy WHERE a.otherMember = :target")
    void reassignOtherMember(@Param("target") Member target, @Param("dummy") Member dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AdminAlert a SET a.requester = :dummy WHERE a.requester = :target")
    void reassignRequester(@Param("target") Member target, @Param("dummy") Member dummy);
}
