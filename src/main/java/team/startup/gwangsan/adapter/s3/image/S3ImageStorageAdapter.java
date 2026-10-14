package team.startup.gwangsan.adapter.s3.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import team.startup.gwangsan.application.image.data.DeleteResult;
import team.startup.gwangsan.application.image.port.out.ImageStoragePort;
import team.startup.gwangsan.domain.image.exception.ImageUploadFailedException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

/**
 * 기존 S3UploadService/S3DeleteService. @Async 는 제거했다: 업로드는 호출자가 결과를 기다렸고, 단건 삭제는 SDK 비동기
 * 호출 자체가 대기 없이 반환되며, 일괄 삭제는 self-call 이라 원래 @Async 가 적용되지 않았다.
 * 삭제 key 로 저장된 URL 을 그대로 넘기는 기존 동작을 유지한다(업로드 key 는 UUID/파일명 — 증거 문서 미해결 항목).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageStoragePort {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private final S3AsyncClient s3AsyncClient;

    @Override
    public String upload(String fileName, byte[] content) {
        String uploadFileName = UUID.randomUUID() + "/" + fileName;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uploadFileName)
                .build();
        try {
            s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromBytes(content)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImageUploadFailedException();
        } catch (ExecutionException | RuntimeException e) {
            throw new ImageUploadFailedException();
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, uploadFileName);
    }

    @Override
    public void delete(String imageUrl) {
        try {
            deleteObject(imageUrl).whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    log.error("파일 삭제에 실패하였습니다: {}", imageUrl, throwable);
                }
            });
        } catch (RuntimeException e) {
            log.error("파일 삭제에 실패하였습니다: {}", imageUrl, e);
        }
    }

    @Override
    public DeleteResult deleteAll(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            log.warn("⚠️ 삭제할 파일이 없습니다.");
            return new DeleteResult(0, 0, List.of());
        }

        log.info("🧹 S3 다중 파일 삭제 시작 (총 {}개)", imageUrls.size());

        var failedQueue = new ConcurrentLinkedQueue<String>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            futures.add(deleteObject(imageUrl).handle((ignored, throwable) -> {
                if (throwable != null) {
                    log.error("파일 삭제에 실패하였습니다: {}", imageUrl, throwable);
                    failedQueue.add(imageUrl);
                }
                return null;
            }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<String> failedFiles = new ArrayList<>(failedQueue);
        int failCount = failedFiles.size();
        int successCount = imageUrls.size() - failCount;

        log.info("✅ S3 다중 파일 삭제 완료 (성공: {}개, 실패: {}개)", successCount, failCount);
        if (!failedFiles.isEmpty()) {
            log.warn("⚠️ 삭제 실패한 파일: {}", failedFiles);
        }
        return new DeleteResult(successCount, failCount, failedFiles);
    }

    private CompletableFuture<?> deleteObject(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3AsyncClient.deleteObject(request);
    }
}
