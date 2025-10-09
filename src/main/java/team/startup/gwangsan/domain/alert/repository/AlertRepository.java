package team.startup.gwangsan.domain.alert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.alert.entity.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {
}
