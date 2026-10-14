package team.startup.gwangsan.adapter.persistence.dong;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DongJpaRepository extends JpaRepository<DongJpaEntity, Integer> {
    Optional<DongJpaEntity> findByName(String name);
}
