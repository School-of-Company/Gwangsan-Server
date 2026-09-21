package team.startup.gwangsan.domain.image.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageUploadFailedException;
import team.startup.gwangsan.domain.image.exception.InappropriateImageException;
import team.startup.gwangsan.domain.image.presentation.dto.response.UploadImageResponse;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.global.thirdparty.aws.s3.service.S3UploadService;
import team.startup.gwangsan.global.thirdparty.ai.AiModerationClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadImageServiceImpl 단위 테스트")
class UploadImageServiceImplTest {

    @InjectMocks private UploadImageServiceImpl service;

    @Mock private S3UploadService s3UploadService;
    @Mock private ImageRepository imageRepository;
    @Mock private AiModerationClient aiModerationClient;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 파일 업로드 요청일 때")
        class Context_with_valid_file {

            @Test
            @DisplayName("이미지를 저장하고 응답을 반환한다")
            void it_saves_image_and_returns_response() throws IOException {
                MultipartFile file = mock(MultipartFile.class);
                when(file.getOriginalFilename()).thenReturn("test.png");
                when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
                when(s3UploadService.execute(eq("test.png"), any()))
                        .thenReturn(CompletableFuture.completedFuture("https://s3.example.com/test.png"));
                when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> {
                    Image image = invocation.getArgument(0);
                    ReflectionTestUtils.setField(image, "id", 42L);
                    return image;
                });

                UploadImageResponse response = service.execute(file);

                ArgumentCaptor<Image> imageCaptor = ArgumentCaptor.forClass(Image.class);
                verify(imageRepository).save(imageCaptor.capture());
                assertThat(imageCaptor.getValue().getImageUrl()).isEqualTo("https://s3.example.com/test.png");
                assertThat(response.imageId()).isEqualTo(42L);
                assertThat(response.imageUrl()).isEqualTo(imageCaptor.getValue().getImageUrl());
            }
        }

        @Nested
        @DisplayName("부적절한 이미지일 때")
        class Context_with_inappropriate_image {

            @Test
            @DisplayName("InappropriateImageException을 던지고 업로드하지 않는다")
            void it_rejects_image_before_upload() {
                MultipartFile file = mock(MultipartFile.class);
                when(aiModerationClient.isNsfw(file)).thenReturn(true);

                assertThatThrownBy(() -> service.execute(file))
                        .isInstanceOf(InappropriateImageException.class);

                verifyNoInteractions(s3UploadService, imageRepository);
            }
        }

        @Nested
        @DisplayName("파일 입력 스트림 읽기에 실패할 때")
        class Context_with_io_exception {

            @Test
            @DisplayName("ImageUploadFailedException을 던진다")
            void it_throws_image_upload_failed_exception() throws IOException {
                MultipartFile file = mock(MultipartFile.class);
                when(file.getOriginalFilename()).thenReturn("test.png");
                when(file.getInputStream()).thenThrow(new IOException("IO error"));

                assertThatThrownBy(() -> service.execute(file))
                        .isInstanceOf(ImageUploadFailedException.class);
            }
        }

        @Nested
        @DisplayName("S3 업로드 비동기 작업이 실패할 때")
        class Context_with_failed_upload_future {

            @Test
            @DisplayName("ImageUploadFailedException을 던지고 이미지를 저장하지 않는다")
            void it_throws_image_upload_failed_exception_without_saving_image() throws IOException {
                MultipartFile file = mock(MultipartFile.class);
                when(file.getOriginalFilename()).thenReturn("test.png");
                when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
                when(s3UploadService.execute(eq("test.png"), any()))
                        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("S3 failure")));

                assertThatThrownBy(() -> service.execute(file))
                        .isInstanceOf(ImageUploadFailedException.class);

                verifyNoInteractions(imageRepository);
            }
        }
    }
}
