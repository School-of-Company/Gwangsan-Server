package team.startup.gwangsan.adapter.persistence.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.util.Optional;

public interface AdminAlertJpaRepository extends JpaRepository<AdminAlertJpaEntity, Long> {

    Optional<AdminAlertJpaEntity> findByIdAndType(Long alertId, AlertType type);

    Optional<AdminAlertJpaEntity> findByTypeAndSourceId(AlertType type, Long sourceId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AdminAlertJpaEntity a SET a.otherMember = :dummy WHERE a.otherMember = :target")
    void reassignOtherMember(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AdminAlertJpaEntity a SET a.requester = :dummy WHERE a.requester = :target")
    void reassignRequester(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
