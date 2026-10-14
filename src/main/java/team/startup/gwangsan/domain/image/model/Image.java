package team.startup.gwangsan.domain.image.model;

import java.time.LocalDateTime;

public record Image(Long id, String imageUrl, LocalDateTime createdAt) {

    public static Image create(String imageUrl) {
        return new Image(null, imageUrl, null);
    }

    public boolean isCreatedBefore(LocalDateTime cutoff) {
        return createdAt != null && createdAt.isBefore(cutoff);
    }
}
