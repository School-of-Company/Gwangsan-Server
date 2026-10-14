package team.startup.gwangsan.adapter.persistence.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageJpaRepository extends JpaRepository<ImageJpaEntity, Long> {
    List<ImageJpaEntity> findByIdIn(List<Long> imageIds);
}
