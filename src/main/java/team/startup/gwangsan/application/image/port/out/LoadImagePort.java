package team.startup.gwangsan.application.image.port.out;

import team.startup.gwangsan.domain.image.model.Image;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadImagePort {
    Optional<Image> findById(Long imageId);

    List<Image> findAllByIds(Collection<Long> imageIds);
}
