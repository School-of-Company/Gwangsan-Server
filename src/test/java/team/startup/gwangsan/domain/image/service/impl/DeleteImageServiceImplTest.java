package team.startup.gwangsan.domain.image.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.global.thirdparty.aws.s3.service.S3DeleteService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteImageServiceImpl 단위 테스트")
class DeleteImageServiceImplTest {

    @InjectMocks
    private DeleteImageServiceImpl service;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private S3DeleteService s3DeleteService;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("S3 삭제 후 이미지를 삭제한다")
            void it_deletes_image_and_s3_object() {
                Image image = mock(Image.class);
                when(image.getImageUrl()).thenReturn("https://s3.example.com/image.png");
                when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

                service.execute(1L);

                verify(s3DeleteService).execute("https://s3.example.com/image.png");
                verify(imageRepository).delete(image);
            }
        }

        @Nested
        @DisplayName("이미지가 존재하지 않을 때")
        class Context_with_not_found_image {

            @Test
            @DisplayName("ImageNotFoundException을 던진다")
            void it_throws_image_not_found_exception() {
                when(imageRepository.findById(1L)).thenReturn(Optional.empty());

                assertThrows(ImageNotFoundException.class, () -> service.execute(1L));
            }
        }
    }
}