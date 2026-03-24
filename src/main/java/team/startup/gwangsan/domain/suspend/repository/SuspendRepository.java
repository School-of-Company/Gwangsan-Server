package team.startup.gwangsan.domain.suspend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.suspend.entity.Suspend;

import java.time.LocalDateTime;
import java.util.List;

public interface SuspendRepository extends JpaRepository<Suspend, Long> {
    List<Suspend> findAllBySuspendedUntilBefore(LocalDateTime until);
}
