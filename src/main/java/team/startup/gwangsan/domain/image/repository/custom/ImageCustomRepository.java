package team.startup.gwangsan.domain.image.repository.custom;

import team.startup.gwangsan.domain.image.entity.Image;

import java.util.List;

public interface ImageCustomRepository {
    List<Image> findAllOrphanImages(int limit);
}
