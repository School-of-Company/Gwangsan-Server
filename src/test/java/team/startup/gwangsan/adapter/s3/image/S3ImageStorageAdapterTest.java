package team.startup.gwangsan.adapter.s3.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import team.startup.gwangsan.application.image.data.DeleteResult;
import team.startup.gwangsan.domain.image.exception.ImageUploadFailedException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("S3ImageStorageAdapter 단위 테스트")
class S3ImageStorageAdapterTest {

    private S3AsyncClient s3AsyncClient;
    private S3ImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        s3AsyncClient = mock(S3AsyncClient.class);
        adapter = new S3ImageStorageAdapter(s3AsyncClient);
        ReflectionTestUtils.setField(adapter, "bucket", "bucket");
        ReflectionTestUtils.setField(adapter, "region", "ap-northeast-2");
    }

    @Nested
    @DisplayName("upload() 는")
    class Describe_upload {

        @Test
        @DisplayName("UUID/파일명 key 로 올리고 기존 형식의 공개 URL 을 반환한다")
        void it_uploads_with_uuid_key_and_returns_url() {
            ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
            when(s3AsyncClient.putObject(request.capture(), any(AsyncRequestBody.class)))
                    .thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()));

            String url = adapter.upload("photo.png", new byte[]{1});

            assertThat(request.getValue().bucket()).isEqualTo("bucket");
            assertThat(request.getValue().key()).matches("[0-9a-f-]{36}/photo\\.png");
            assertThat(url).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/" + request.getValue().key());
        }

        @Test
        @DisplayName("비동기 업로드 실패는 ImageUploadFailedException 이다")
        void it_maps_failed_future() {
            when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("S3 failure")));

            assertThatThrownBy(() -> adapter.upload("photo.png", new byte[]{1}))
                    .isInstanceOf(ImageUploadFailedException.class);
        }
    }

    @Nested
    @DisplayName("delete() 는")
    class Describe_delete {

        @Test
        @DisplayName("실패(동기 예외 포함)를 호출자에게 전파하지 않는다")
        void it_never_propagates_failure() {
            when(s3AsyncClient.deleteObject(any(DeleteObjectRequest.class)))
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("async")))
                    .thenThrow(new IllegalStateException("sync"));

            assertThatCode(() -> adapter.delete("a")).doesNotThrowAnyException();
            assertThatCode(() -> adapter.delete("b")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("deleteAll() 은")
    class Describe_deleteAll {

        @Test
        @DisplayName("저장된 URL 을 key 로 지우고 성공·실패 수와 실패 URL 을 모은다")
        void it_collects_failures() {
            ArgumentCaptor<DeleteObjectRequest> requests = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            when(s3AsyncClient.deleteObject(requests.capture()))
                    .thenReturn(CompletableFuture.completedFuture(DeleteObjectResponse.builder().build()))
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("S3 failure")));

            DeleteResult result = adapter.deleteAll(List.of("ok", "fail"));

            assertThat(result).isEqualTo(new DeleteResult(1, 1, List.of("fail")));
            assertThat(requests.getAllValues()).extracting(DeleteObjectRequest::key).containsExactly("ok", "fail");
        }

        @Test
        @DisplayName("빈 목록은 S3 를 호출하지 않는다")
        void it_skips_empty_list() {
            assertThat(adapter.deleteAll(List.of())).isEqualTo(new DeleteResult(0, 0, List.of()));
            verifyNoInteractions(s3AsyncClient);
        }
    }
}
