package team.startup.gwangsan.global.thirdparty.aws.s3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private final S3AsyncClient s3AsyncClient;

    @Async
    public CompletableFuture<String> execute(String fileName, InputStream inputStream) {
        String uploadFileName = UUID.randomUUID() + "/" + fileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(uploadFileName)
                    .build();

            AsyncRequestBody requestBody = AsyncRequestBody.fromBytes(inputStream.readAllBytes());
            CompletableFuture<PutObjectResponse> responseFuture = s3AsyncClient.putObject(putObjectRequest, requestBody);
            return responseFuture.thenApply(response -> String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, uploadFileName));
        } catch (IOException e) {
            // TODO: 커스텀 예외로 변경하기
            throw new RuntimeException(e);
        }

    }
}
