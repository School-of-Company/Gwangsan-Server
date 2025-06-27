package team.startup.gwangsan.global.event;

import team.startup.gwangsan.global.event.constant.ImageType;

import java.util.Set;

public record DeleteNotUsedImageEvent(
        Long sourceId,
        Set<Long> imageIds,
        ImageType type
) {
}
