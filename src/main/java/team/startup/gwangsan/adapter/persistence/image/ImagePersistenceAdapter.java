package team.startup.gwangsan.adapter.persistence.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.image.port.out.DeleteImagePort;
import team.startup.gwangsan.application.image.port.out.LoadImagePort;
import team.startup.gwangsan.application.image.port.out.SaveImagePort;
import team.startup.gwangsan.domain.image.model.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// bridge: JPA Entity(domain.image.entity.Image)·ImageRepository 는 5개 업무 Entity association 과 legacy 서비스가 직접 쓰므로
// 위치를 유지한다. adapter/persistence/image 로의 이동은 PR12(소비 업무 전환 후).
@Component
@RequiredArgsConstructor
public class ImagePersistenceAdapter implements LoadImagePort, SaveImagePort, DeleteImagePort {

    private final ImageRepository imageRepository;

    @Override
    public Optional<Image> findById(Long imageId) {
        return imageRepository.findById(imageId).map(ImagePersistenceAdapter::toModel);
    }

    @Override
    public List<Image> findAllByIds(Collection<Long> imageIds) {
        return imageRepository.findAllById(imageIds).stream().map(ImagePersistenceAdapter::toModel).toList();
    }

    @Override
    public Image save(Image image) {
        return toModel(imageRepository.save(team.startup.gwangsan.domain.image.entity.Image.builder()
                .imageUrl(image.imageUrl())
                .build()));
    }

    @Override
    public void deleteById(Long imageId) {
        imageRepository.deleteById(imageId);
    }

    @Override
    public void deleteAllByIdInBatch(List<Long> imageIds) {
        imageRepository.deleteAllByIdInBatch(imageIds);
    }

    static Image toModel(team.startup.gwangsan.domain.image.entity.Image entity) {
        return new Image(entity.getId(), entity.getImageUrl(), entity.getCreatedAt());
    }
}
