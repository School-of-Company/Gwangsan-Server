package team.startup.gwangsan.domain.image.repository;

import org.springframework.data.repository.CrudRepository;
import team.startup.gwangsan.domain.image.entity.Image;

import java.util.List;

public interface ImageRepository extends CrudRepository<Image, Long> {
    List<Image> findByImageUrlIn(List<String> imageUrls);

    List<Image> findImagesById(Long id);
}
