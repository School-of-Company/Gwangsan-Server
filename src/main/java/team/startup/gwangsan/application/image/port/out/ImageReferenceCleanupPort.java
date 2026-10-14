package team.startup.gwangsan.application.image.port.out;

import team.startup.gwangsan.application.image.data.ImageOwnerType;

public interface ImageReferenceCleanupPort {
    void unlink(ImageOwnerType ownerType, Long sourceId, Long imageId);
}
