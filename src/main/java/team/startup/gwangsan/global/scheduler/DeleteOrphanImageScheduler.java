package team.startup.gwangsan.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.entity.dto.DeleteResult;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.global.thirdparty.aws.s3.service.S3DeleteService;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteOrphanImageScheduler {

    private final ImageRepository imageRepository;
    private final S3DeleteService s3DeleteService;

    private static final int MAX_DELETE_COUNT = 500;
    private static final int MIN_AGE_DAYS = 2;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void deleteOrphanImages() {
        log.info("🧹 [이미지 정리] 고아 이미지 정리 스케줄러 시작");

        long start = System.currentTimeMillis();

        try {
            List<Image> orphanImages = findOrphanImages();
            if (orphanImages.isEmpty()) {
                log.info("✅ [이미지 정리] 삭제할 고아 이미지가 없습니다. (조회 시간: {}ms)",
                        System.currentTimeMillis() - start);
                return;
            }

            long queryEnd = System.currentTimeMillis();

            List<String> s3Keys = orphanImages.stream()
                    .map(Image::getImageUrl)
                    .collect(Collectors.toList());

            DeleteResult s3Result = s3DeleteService.deleteAll(s3Keys);
            long s3DeleteEnd = System.currentTimeMillis();

            int dbDeletedCount = deleteFromDatabase(orphanImages, s3Result);
            long dbDeleteEnd = System.currentTimeMillis();

            logResult(orphanImages.size(), s3Result, dbDeletedCount, start, queryEnd, s3DeleteEnd, dbDeleteEnd);

        } catch (Exception e) {
            log.error("❌ [이미지 정리] 고아 이미지 정리 중 오류 발생", e);
        }

        log.info("🧹 [이미지 정리] 고아 이미지 정리 스케줄러 종료 (총 소요: {}ms)",
                System.currentTimeMillis() - start);
    }

    @Transactional(readOnly = true)
    protected List<Image> findOrphanImages() {
        List<Image> candidates = imageRepository.findAllOrphanImages(MAX_DELETE_COUNT);

        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(MIN_AGE_DAYS);

        List<Image> filtered = candidates.stream()
                .filter(img -> {
                    LocalDateTime createdAt = img.getCreatedAt();
                    return createdAt != null && createdAt.isBefore(cutoff);
                })
                .collect(Collectors.toList());

        if (candidates.size() != filtered.size()) {
            log.info("ℹ️ [이미지 정리] 생성 후 {}일 미만 이미지는 보존합니다. (후보 {}건 → 삭제대상 {}건)", MIN_AGE_DAYS, candidates.size(), filtered.size());
        }

        return filtered;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class, timeout = 60)
    protected int deleteFromDatabase(List<Image> orphanImages, DeleteResult s3Result) {
        if (s3Result.successCount() == 0) {
            log.warn("⚠️ [이미지 정리] S3에서 삭제된 파일이 없어 DB 삭제를 건너뜁니다.");
            return 0;
        }

        final LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(MIN_AGE_DAYS);

        List<Image> successfullyDeleted = orphanImages.stream()
                .filter(image -> !s3Result.failedImageUrls().contains(image.getImageUrl()))
                .filter(image -> {
                    LocalDateTime createdAt = image.getCreatedAt();
                    return createdAt != null && createdAt.isBefore(cutoff);
                })
                .toList();

        if (!successfullyDeleted.isEmpty()) {
            final int batchSize = 200;
            List<Image> batch = new ArrayList<>(batchSize);
            for (Image img : successfullyDeleted) {
                batch.add(img);
                if (batch.size() == batchSize) {
                    imageRepository.deleteAllInBatch(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                imageRepository.deleteAllInBatch(batch);
            }
            log.info("✅ [이미지 정리] DB에서 {}개 이미지 삭제 완료 (2일 미만 보존 규칙 적용)", successfullyDeleted.size());
        }

        return successfullyDeleted.size();
    }

    private void logResult(int totalFound, DeleteResult s3Result, int dbDeleted,
                           long start, long queryEnd, long s3End, long dbEnd) {
        log.info("""
                
                ========================================
                🗑️ [이미지 정리] 고아 이미지 삭제 결과
                ========================================
                📊 조회: {}개 발견
                ☁️ S3: 성공 {}개, 실패 {}개
                💾 DB: 삭제 {}개
                ⏱️ 성능:
                   - 조회: {}ms
                   - S3 삭제: {}ms
                   - DB 삭제: {}ms
                   - 총 소요: {}ms
                ========================================
                """,
                totalFound,
                s3Result.successCount(), s3Result.failCount(),
                dbDeleted,
                queryEnd - start,
                s3End - queryEnd,
                dbEnd - s3End,
                dbEnd - start);

        if (s3Result.hasFailures()) {
            log.error("⚠️ [이미지 정리] S3 삭제 실패 파일: {}", s3Result.failedImageUrls());
        }
    }
}