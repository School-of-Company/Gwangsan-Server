package team.startup.gwangsan.application.image.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.image.data.DeleteImageCommand;
import team.startup.gwangsan.application.image.port.out.DeleteImagePort;
import team.startup.gwangsan.application.image.port.out.ImageStoragePort;
import team.startup.gwangsan.application.image.port.out.LoadImagePort;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.model.Image;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteImageService 단위 테스트")
class DeleteImageServiceTest {

    @InjectMocks private DeleteImageService service;

    @Mock private LoadImagePort loadImagePort;
    @Mock private ImageStoragePort imageStoragePort;
    @Mock private DeleteImagePort deleteImagePort;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("저장소 삭제 요청 후 DB 에서 이미지를 삭제한다")
        void it_deletes_storage_object_then_image() {
            when(loadImagePort.findById(1L))
                    .thenReturn(Optional.of(new Image(1L, "https://s3.example.com/image.png", null)));

            service.execute(new DeleteImageCommand(1L));

            InOrder order = inOrder(imageStoragePort, deleteImagePort);
            order.verify(imageStoragePort).delete("https://s3.example.com/image.png");
            order.verify(deleteImagePort).deleteById(1L);
        }

        @Test
        @DisplayName("이미지가 없으면 ImageNotFoundException 을 던지고 아무것도 지우지 않는다")
        void it_throws_image_not_found_exception() {
            when(loadImagePort.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new DeleteImageCommand(1L)))
                    .isInstanceOf(ImageNotFoundException.class);

            verifyNoInteractions(imageStoragePort, deleteImagePort);
        }
    }
}
