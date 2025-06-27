package team.startup.gwangsan.global.util;

import lombok.experimental.UtilityClass;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class ImageValidateUtil {

    // 클라이언트가 보낸 imageId 들 중 DB에 존재하지 않는 image가 있을 시에 예외 발생
    public void validateExistence(List<Long> requestedIds, List<Image> foundImages) {
        Set<Long> foundIds = foundImages.stream()
                .map(Image::getId)
                .collect(Collectors.toSet());

        List<Long> notFound = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!notFound.isEmpty()) {
            throw new ImageNotFoundException();
        }
    }
}
