package team.startup.gwangsan.application.image.data;

import java.util.Set;

public record DeleteUnusedImagesCommand(Long sourceId, Set<Long> imageIds, ImageOwnerType ownerType) {
}
