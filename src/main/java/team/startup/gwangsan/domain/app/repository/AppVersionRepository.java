package team.startup.gwangsan.domain.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.app.entity.AppVersion;

public interface AppVersionRepository extends JpaRepository<AppVersion, String> {
}
