package team.startup.gwangsan.domain.image.entity.dto;

import java.util.List;

public record DeleteResult(
        int successCount,
        int failCount,
        List<String> failedImageUrls
) {
    public boolean hasFailures() {
        return failCount > 0;
    }
}
