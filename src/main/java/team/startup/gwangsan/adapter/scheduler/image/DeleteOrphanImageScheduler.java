package team.startup.gwangsan.adapter.scheduler.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.image.port.in.CleanupOrphanImagesUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteOrphanImageScheduler {

    private final CleanupOrphanImagesUseCase cleanupOrphanImagesUseCase;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void deleteOrphanImages() {
        log.info("🧹 [이미지 정리] 고아 이미지 정리 스케줄러 시작");
        long start = System.currentTimeMillis();

        try {
            cleanupOrphanImagesUseCase.execute();
        } catch (Exception e) {
            log.error("❌ [이미지 정리] 고아 이미지 정리 중 오류 발생", e);
        }

        log.info("🧹 [이미지 정리] 고아 이미지 정리 스케줄러 종료 (총 소요: {}ms)",
                System.currentTimeMillis() - start);
    }
}
