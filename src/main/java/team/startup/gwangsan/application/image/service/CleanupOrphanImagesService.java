package team.startup.gwangsan.application.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.image.data.DeleteResult;
import team.startup.gwangsan.application.image.port.in.CleanupOrphanImagesUseCase;
import team.startup.gwangsan.application.image.port.out.DeleteImagePort;
import team.startup.gwangsan.application.image.port.out.ImageStoragePort;
import team.startup.gwangsan.application.image.port.out.LoadOrphanImageCandidatesPort;
import team.startup.gwangsan.domain.image.model.Image;
import team.startup.gwangsan.domain.image.policy.OrphanImagePolicy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 트랜잭션을 열지 않는다. 기준선 스케줄러의 @Transactional 은 self-call 이라 적용되지 않았고, 조회와 200건 배치 삭제가
 * 각각 저장소 트랜잭션으로 커밋됐다. S3 호출을 DB 트랜잭션 밖에 두고 배치별 커밋을 유지하기 위해 그대로 둔다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupOrphanImagesService implements CleanupOrphanImagesUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DB_BATCH_SIZE = 200;

    private final LoadOrphanImageCandidatesPort loadOrphanImageCandidatesPort;
    private final ImageStoragePort imageStoragePort;
    private final DeleteImagePort deleteImagePort;

    @Override
    public void execute() {
        long start = System.currentTimeMillis();
        // 기존에는 cutoff 를 두 번 계산했지만 두 번째 필터는 첫 필터의 상위 조건이라 1회 계산과 결과가 같다.
        LocalDateTime cutoff = OrphanImagePolicy.cutoff(LocalDateTime.now(SEOUL));

        List<Image> candidates = loadOrphanImageCandidatesPort.findOrphanCandidates(OrphanImagePolicy.MAX_DELETE_COUNT);
        List<Image> expired = OrphanImagePolicy.expired(candidates, cutoff);
        if (candidates.size() != expired.size()) {
            log.info("ℹ️ [이미지 정리] 생성 후 {}일 미만 이미지는 보존합니다. (후보 {}건 → 삭제대상 {}건)",
                    OrphanImagePolicy.MIN_AGE_DAYS, candidates.size(), expired.size());
        }
        if (expired.isEmpty()) {
            log.info("✅ [이미지 정리] 삭제할 고아 이미지가 없습니다. (조회 시간: {}ms)", System.currentTimeMillis() - start);
            return;
        }
        long queryEnd = System.currentTimeMillis();

        DeleteResult storageResult = imageStoragePort.deleteAll(expired.stream().map(Image::imageUrl).toList());
        long storageEnd = System.currentTimeMillis();

        int dbDeleted = deleteFromDatabase(expired, storageResult);
        long dbEnd = System.currentTimeMillis();

        logResult(expired.size(), storageResult, dbDeleted, start, queryEnd, storageEnd, dbEnd);
    }

    private int deleteFromDatabase(List<Image> expired, DeleteResult storageResult) {
        if (storageResult.successCount() == 0) {
            log.warn("⚠️ [이미지 정리] S3에서 삭제된 파일이 없어 DB 삭제를 건너뜁니다.");
            return 0;
        }

        List<Long> ids = OrphanImagePolicy.deletable(expired, storageResult.failedImageUrls()).stream()
                .map(Image::id)
                .toList();
        for (int from = 0; from < ids.size(); from += DB_BATCH_SIZE) {
            deleteImagePort.deleteAllByIdInBatch(List.copyOf(ids.subList(from, Math.min(from + DB_BATCH_SIZE, ids.size()))));
        }
        if (!ids.isEmpty()) {
            log.info("✅ [이미지 정리] DB에서 {}개 이미지 삭제 완료 (2일 미만 보존 규칙 적용)", ids.size());
        }
        return ids.size();
    }

    private void logResult(int totalFound, DeleteResult storageResult, int dbDeleted,
                           long start, long queryEnd, long storageEnd, long dbEnd) {
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
                storageResult.successCount(), storageResult.failCount(),
                dbDeleted,
                queryEnd - start,
                storageEnd - queryEnd,
                dbEnd - storageEnd,
                dbEnd - start);

        if (storageResult.hasFailures()) {
            log.error("⚠️ [이미지 정리] S3 삭제 실패 파일: {}", storageResult.failedImageUrls());
        }
    }
}
