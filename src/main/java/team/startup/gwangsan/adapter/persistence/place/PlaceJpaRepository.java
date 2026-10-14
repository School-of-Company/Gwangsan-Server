package team.startup.gwangsan.adapter.persistence.place;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceJpaRepository extends JpaRepository<PlaceJpaEntity, Integer> {
    List<PlaceJpaEntity> findAllByHeadId(Integer headId);
}
