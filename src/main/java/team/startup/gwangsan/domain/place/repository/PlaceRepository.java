package team.startup.gwangsan.domain.place.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Integer> {
    Optional<Place> findByName(String name);
}
