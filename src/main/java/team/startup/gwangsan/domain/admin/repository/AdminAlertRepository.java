package team.startup.gwangsan.domain.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;

import java.util.Optional;

public interface AdminAlertRepository extends JpaRepository<AdminAlert, Long> {
    Optional<AdminAlert> findBySourceId(Long sourceId);

    Optional<AdminAlert> findBySourceIdAndType(Long sourceId, AlertType type);
}
