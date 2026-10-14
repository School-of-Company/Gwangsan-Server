package team.startup.gwangsan.application.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;
import team.startup.gwangsan.application.image.port.out.LoadImagePort;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageQueryService implements ImageQuery {

    private final LoadImagePort loadImagePort;

    @Override
    @Transactional(readOnly = true)
    public List<ImageSummary> findAllByIds(Collection<Long> imageIds) {
        return loadImagePort.findAllByIds(imageIds).stream()
                .map(image -> new ImageSummary(image.id(), image.imageUrl()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImageSummary> getAllByIds(List<Long> imageIds) {
        List<ImageSummary> found = findAllByIds(imageIds);
        Set<Long> foundIds = found.stream().map(ImageSummary::imageId).collect(Collectors.toSet());
        if (!foundIds.containsAll(imageIds)) {
            throw new ImageNotFoundException();
        }
        return found;
    }
}
