package team.startup.gwangsan.domain.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.repository.custom.AdminAlertCustomRepository;

import java.util.Optional;

public interface AdminAlertRepository extends JpaRepository<AdminAlert, Long>, AdminAlertCustomRepository {
    Optional<AdminAlert> findBySourceId(Long sourceId);

    Optional<AdminAlert> findByIdAndType(Long alertId, AlertType type);
}
