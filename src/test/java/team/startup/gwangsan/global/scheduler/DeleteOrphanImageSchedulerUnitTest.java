package team.startup.gwangsan.global.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.entity.dto.DeleteResult;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.global.thirdparty.aws.s3.service.S3DeleteService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteOrphanImageScheduler 단위 테스트")
class DeleteOrphanImageSchedulerUnitTest {

    private static final LocalDateTime OLD = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime FUTURE = LocalDateTime.of(2999, 1, 1, 0, 0);

    private ImageRepository imageRepository;
    private S3DeleteService s3DeleteService;
    private DeleteOrphanImageScheduler scheduler;

    @BeforeEach
    void setUp() {
        imageRepository = mock(ImageRepository.class);
        s3DeleteService = mock(S3DeleteService.class);
        scheduler = new DeleteOrphanImageScheduler(imageRepository, s3DeleteService);
    }

    @Nested
    @DisplayName("deleteOrphanImages() 메서드는")
    class Describe_deleteOrphanImages {

        @Test
        @DisplayName("S3 부분 삭제 성공 시 오래된 성공 이미지들만 DB에서 삭제한다")
        void it_deletes_only_successfully_removed_old_images() {
            Image oldSuccess = image("old-success", OLD);
            Image oldFailure = image("old-failure", OLD);
            Image future = image("future", FUTURE);
            Image missingCreatedAt = image("missing-created-at", null);
            when(imageRepository.findAllOrphanImages(500))
                    .thenReturn(List.of(oldSuccess, oldFailure, future, missingCreatedAt));
            when(s3DeleteService.deleteAll(List.of(oldSuccess.getImageUrl(), oldFailure.getImageUrl())))
                    .thenReturn(new DeleteResult(1, 1, List.of(oldFailure.getImageUrl())));

            scheduler.deleteOrphanImages();

            verify(imageRepository).deleteAllInBatch(List.of(oldSuccess));
            verify(imageRepository).findAllOrphanImages(500);
            verifyNoMoreInteractions(imageRepository);
            verify(s3DeleteService).deleteAll(List.of(oldSuccess.getImageUrl(), oldFailure.getImageUrl()));
        }

        @Test
        @DisplayName("모든 S3 삭제가 실패하면 DB 삭제를 수행하지 않는다")
        void it_skips_database_deletion_when_all_s3_deletions_fail() {
            Image first = image("first", OLD);
            Image second = image("second", OLD);
            when(imageRepository.findAllOrphanImages(500)).thenReturn(List.of(first, second));
            when(s3DeleteService.deleteAll(List.of(first.getImageUrl(), second.getImageUrl())))
                    .thenReturn(new DeleteResult(0, 2, List.of(first.getImageUrl(), second.getImageUrl())));

            scheduler.deleteOrphanImages();

            verify(imageRepository).findAllOrphanImages(500);
            verifyNoMoreInteractions(imageRepository);
            verify(s3DeleteService).deleteAll(List.of(first.getImageUrl(), second.getImageUrl()));
        }

        @Test
        @DisplayName("삭제 대상이 없으면 S3를 호출하지 않는다")
        void it_does_not_call_s3_when_no_candidate_is_old_enough() {
            when(imageRepository.findAllOrphanImages(500))
                    .thenReturn(List.of(image("future", FUTURE), image("missing-created-at", null)));

            scheduler.deleteOrphanImages();

            verify(imageRepository).findAllOrphanImages(500);
            verifyNoMoreInteractions(imageRepository);
            verifyNoInteractions(s3DeleteService);
        }

        @Test
        @DisplayName("S3 삭제 예외가 발생하면 DB 삭제를 시작하지 않는다")
        void it_preserves_database_images_when_s3_deletion_throws() {
            Image oldImage = image("old", OLD);
            when(imageRepository.findAllOrphanImages(500)).thenReturn(List.of(oldImage));
            doThrow(new IllegalStateException("S3 unavailable"))
                    .when(s3DeleteService).deleteAll(List.of(oldImage.getImageUrl()));

            scheduler.deleteOrphanImages();

            verify(imageRepository).findAllOrphanImages(500);
            verifyNoMoreInteractions(imageRepository);
            verify(s3DeleteService).deleteAll(List.of(oldImage.getImageUrl()));
        }
    }

    @Nested
    @DisplayName("deleteFromDatabase() 메서드는")
    class Describe_deleteFromDatabase {

        @Test
        @DisplayName("201개 성공 이미지를 200개와 1개의 독립된 배치로 전달한다")
        void it_copies_each_batch_payload_before_the_mutable_batch_is_cleared() {
            List<Image> images = new ArrayList<>();
            for (int index = 0; index < 201; index++) {
                images.add(image("old-" + index, OLD));
            }
            List<List<Image>> deletedBatches = new ArrayList<>();
            doAnswer(invocation -> {
                List<Image> batch = invocation.getArgument(0);
                deletedBatches.add(List.copyOf(batch));
                return null;
            }).when(imageRepository).deleteAllInBatch(anyList());

            int deletedCount = scheduler.deleteFromDatabase(images, new DeleteResult(201, 0, List.of()));

            assertThat(deletedCount).isEqualTo(201);
            assertThat(deletedBatches).hasSize(2);
            assertThat(deletedBatches.getFirst()).containsExactlyElementsOf(images.subList(0, 200));
            assertThat(deletedBatches.get(1)).containsExactly(images.get(200));
        }
    }

    private Image image(String name, LocalDateTime createdAt) {
        Image image = Image.builder().imageUrl("scheduler/" + name).build();
        if (createdAt != null) {
            ReflectionTestUtils.setField(image, "createdAt", createdAt);
        }
        return image;
    }
}
