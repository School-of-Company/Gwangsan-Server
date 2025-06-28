package team.startup.gwangsan.global.thirdparty.aws.s3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.util.concurrent.CompletableFuture;

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
        return s3AsyncClient.deleteObject(deleteObjectRequest).thenAccept(response -> log.info("파일이 성공적으로 삭제되었습니다: {}", uploadFileName)
        ).exceptionally(throwable -> {
            log.error("파일 삭제에 실패하였습니다: {}", uploadFileName, throwable);
            // TODO: 커스텀 예외로 변경하기
            throw new RuntimeException("파일 삭제에 실패했습니다.");
        });
    }
}
