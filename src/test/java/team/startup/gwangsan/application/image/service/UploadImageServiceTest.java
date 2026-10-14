package team.startup.gwangsan.application.image.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.image.data.UploadImageCommand;
import team.startup.gwangsan.application.image.data.UploadImageResult;
import team.startup.gwangsan.application.image.port.out.ImageModerationPort;
import team.startup.gwangsan.application.image.port.out.ImageStoragePort;
import team.startup.gwangsan.application.image.port.out.SaveImagePort;
import team.startup.gwangsan.domain.image.exception.ImageUploadFailedException;
import team.startup.gwangsan.domain.image.exception.InappropriateImageException;
import team.startup.gwangsan.domain.image.model.Image;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadImageService 단위 테스트")
class UploadImageServiceTest {

    private static final byte[] CONTENT = {1, 2, 3};
    private static final UploadImageCommand COMMAND = new UploadImageCommand("test.png", CONTENT);

    @InjectMocks private UploadImageService service;

    @Mock private ImageModerationPort imageModerationPort;
    @Mock private ImageStoragePort imageStoragePort;
    @Mock private SaveImagePort saveImagePort;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("검사 → 업로드 → 저장 순서로 실행하고 저장된 ID 와 URL 을 반환한다")
        void it_saves_image_and_returns_result() {
            when(imageStoragePort.upload("test.png", CONTENT)).thenReturn("https://s3.example.com/test.png");
            when(saveImagePort.save(Image.create("https://s3.example.com/test.png")))
                    .thenReturn(new Image(42L, "https://s3.example.com/test.png", LocalDateTime.now()));

            UploadImageResult result = service.execute(COMMAND);

            assertThat(result).isEqualTo(new UploadImageResult(42L, "https://s3.example.com/test.png"));
            InOrder order = inOrder(imageModerationPort, imageStoragePort, saveImagePort);
            order.verify(imageModerationPort).isNsfw("test.png", CONTENT);
            order.verify(imageStoragePort).upload("test.png", CONTENT);
            order.verify(saveImagePort).save(Image.create("https://s3.example.com/test.png"));
        }

        @Test
        @DisplayName("부적절한 이미지면 InappropriateImageException 을 던지고 업로드하지 않는다")
        void it_rejects_image_before_upload() {
            when(imageModerationPort.isNsfw("test.png", CONTENT)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(COMMAND)).isInstanceOf(InappropriateImageException.class);

            verifyNoInteractions(imageStoragePort, saveImagePort);
        }

        @Test
        @DisplayName("저장소 업로드가 실패하면 ImageUploadFailedException 을 그대로 던지고 DB 에 저장하지 않는다")
        void it_throws_image_upload_failed_exception_without_saving_image() {
            when(imageStoragePort.upload("test.png", CONTENT)).thenThrow(new ImageUploadFailedException());

            assertThatThrownBy(() -> service.execute(COMMAND)).isInstanceOf(ImageUploadFailedException.class);

            verifyNoInteractions(saveImagePort);
        }
    }
}
