package team.startup.gwangsan.adapter.persistence.app;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppVersionJpaRepository extends JpaRepository<AppVersionJpaEntity, String> {
}
