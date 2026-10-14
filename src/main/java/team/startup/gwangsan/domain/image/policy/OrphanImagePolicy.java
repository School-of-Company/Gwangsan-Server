package team.startup.gwangsan.domain.image.policy;

import team.startup.gwangsan.domain.image.model.Image;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 고아 이미지 정리 규칙. 후보는 SQL limit 적용 후 메모리에서 생성 후 2일 경과 여부로 거른다(기존 순서 유지).
 */
public final class OrphanImagePolicy {

    public static final int MAX_DELETE_COUNT = 500;
    public static final int MIN_AGE_DAYS = 2;

    private OrphanImagePolicy() {
    }

    public static LocalDateTime cutoff(LocalDateTime now) {
        return now.minusDays(MIN_AGE_DAYS);
    }

    public static List<Image> expired(List<Image> candidates, LocalDateTime cutoff) {
        return candidates.stream().filter(image -> image.isCreatedBefore(cutoff)).toList();
    }

    public static List<Image> deletable(List<Image> expired, Collection<String> failedUrls) {
        Set<String> failed = new HashSet<>(failedUrls);
        return expired.stream().filter(image -> !failed.contains(image.imageUrl())).toList();
    }
}
