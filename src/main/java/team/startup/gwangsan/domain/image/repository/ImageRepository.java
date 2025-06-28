package team.startup.gwangsan.domain.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.image.entity.Image;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByIdIn(List<Long> imageIds);

    List<Image> findImagesById(Long id);


}
