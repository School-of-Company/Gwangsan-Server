package team.startup.gwangsan.global.thirdparty.aws.s3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import team.startup.gwangsan.domain.image.entity.dto.DeleteResult;
import team.startup.gwangsan.domain.image.exception.ImageDeleteFailedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3DeleteService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private final S3AsyncClient s3AsyncClient;

    @Async
    public CompletableFuture<Void> execute(String uploadFileName) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(uploadFileName)
                .build();
        return s3AsyncClient.deleteObject(deleteObjectRequest).thenAccept(response -> log.debug("파일이 성공적으로 삭제되었습니다: {}", uploadFileName)
        ).exceptionally(throwable -> {
            log.error("파일 삭제에 실패하였습니다: {}", uploadFileName, throwable);
            throw new ImageDeleteFailedException();
        });
    }

    public DeleteResult deleteAll(List<String> uploadFileNames) {
        if (uploadFileNames == null || uploadFileNames.isEmpty()) {
            log.warn("⚠️ 삭제할 파일이 없습니다.");
            return new DeleteResult(0, 0, List.of());
        }

        log.info("🧹 S3 다중 파일 삭제 시작 (총 {}개)", uploadFileNames.size());

        var failedQueue = new ConcurrentLinkedQueue<String>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String fileName : uploadFileNames) {
            futures.add(execute(fileName).exceptionally(throwable -> {
                failedQueue.add(fileName);
                return null;
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<String> failedFiles = new ArrayList<>(failedQueue);
        int failCount = failedFiles.size();
        int successCount = uploadFileNames.size() - failCount;

        log.info("✅ S3 다중 파일 삭제 완료 (성공: {}개, 실패: {}개)", successCount, failCount);

        if (!failedFiles.isEmpty()) {
            log.warn("⚠️ 삭제 실패한 파일: {}", failedFiles);
        }

        return new DeleteResult(successCount, failCount, failedFiles);
    }
}
