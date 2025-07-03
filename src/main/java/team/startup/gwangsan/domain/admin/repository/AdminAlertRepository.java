package team.startup.gwangsan.domain.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;

public interface AdminAlertRepository extends JpaRepository<AdminAlert, Long> {
}
