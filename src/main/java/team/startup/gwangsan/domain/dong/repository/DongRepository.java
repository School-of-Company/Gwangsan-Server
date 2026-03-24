package team.startup.gwangsan.domain.dong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.dong.entity.Dong;

import java.util.Optional;

public interface DongRepository extends JpaRepository<Dong, Integer> {
    Optional<Dong> findByName(String name);
}
