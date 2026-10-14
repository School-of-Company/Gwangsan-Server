package team.startup.gwangsan.application.image.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.image.data.DeleteResult;
import team.startup.gwangsan.application.image.port.out.DeleteImagePort;
import team.startup.gwangsan.application.image.port.out.ImageStoragePort;
import team.startup.gwangsan.application.image.port.out.LoadOrphanImageCandidatesPort;
import team.startup.gwangsan.domain.image.model.Image;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("CleanupOrphanImagesService 단위 테스트")
class CleanupOrphanImagesServiceTest {

    private static final LocalDateTime OLD = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime FUTURE = LocalDateTime.of(2999, 1, 1, 0, 0);

    private LoadOrphanImageCandidatesPort candidatesPort;
    private ImageStoragePort imageStoragePort;
    private DeleteImagePort deleteImagePort;
    private CleanupOrphanImagesService service;

    @BeforeEach
    void setUp() {
        candidatesPort = mock(LoadOrphanImageCandidatesPort.class);
        imageStoragePort = mock(ImageStoragePort.class);
        deleteImagePort = mock(DeleteImagePort.class);
        service = new CleanupOrphanImagesService(candidatesPort, imageStoragePort, deleteImagePort);
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("limit 500 후보 중 오래된 것만 저장소에서 지우고, 저장소 삭제에 성공한 것만 DB 에서 지운다")
        void it_deletes_only_successfully_removed_old_images() {
            Image oldSuccess = image(1L, "old-success", OLD);
            Image oldFailure = image(2L, "old-failure", OLD);
            when(candidatesPort.findOrphanCandidates(500))
                    .thenReturn(List.of(oldSuccess, oldFailure, image(3L, "future", FUTURE), image(4L, "missing", null)));
            when(imageStoragePort.deleteAll(List.of(oldSuccess.imageUrl(), oldFailure.imageUrl())))
                    .thenReturn(new DeleteResult(1, 1, List.of(oldFailure.imageUrl())));

            service.execute();

            verify(deleteImagePort).deleteAllByIdInBatch(List.of(1L));
            verifyNoMoreInteractions(deleteImagePort);
        }

        @Test
        @DisplayName("모든 저장소 삭제가 실패하면 DB 삭제를 하지 않는다")
        void it_skips_database_deletion_when_all_storage_deletions_fail() {
            Image first = image(1L, "first", OLD);
            Image second = image(2L, "second", OLD);
            when(candidatesPort.findOrphanCandidates(500)).thenReturn(List.of(first, second));
            when(imageStoragePort.deleteAll(anyList()))
                    .thenReturn(new DeleteResult(0, 2, List.of(first.imageUrl(), second.imageUrl())));

            service.execute();

            verifyNoInteractions(deleteImagePort);
        }

        @Test
        @DisplayName("오래된 후보가 없으면 저장소를 호출하지 않는다")
        void it_does_not_call_storage_when_no_candidate_is_old_enough() {
            when(candidatesPort.findOrphanCandidates(500))
                    .thenReturn(List.of(image(1L, "future", FUTURE), image(2L, "missing", null)));

            service.execute();

            verifyNoInteractions(imageStoragePort, deleteImagePort);
        }

        @Test
        @DisplayName("저장소 삭제 예외는 전파하고 DB 삭제를 시작하지 않는다")
        void it_propagates_storage_exception_without_database_deletion() {
            when(candidatesPort.findOrphanCandidates(500)).thenReturn(List.of(image(1L, "old", OLD)));
            when(imageStoragePort.deleteAll(anyList())).thenThrow(new IllegalStateException("S3 unavailable"));

            assertThatThrownBy(() -> service.execute()).isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(deleteImagePort);
        }

        @Test
        @DisplayName("201개 성공 이미지를 200개와 1개의 독립된 배치로 지운다")
        void it_deletes_in_batches_of_two_hundred() {
            List<Image> images = new ArrayList<>();
            for (long id = 1; id <= 201; id++) {
                images.add(image(id, "old-" + id, OLD));
            }
            when(candidatesPort.findOrphanCandidates(500)).thenReturn(images);
            when(imageStoragePort.deleteAll(anyList())).thenReturn(new DeleteResult(201, 0, List.of()));
            List<List<Long>> batches = new ArrayList<>();
            doAnswer(invocation -> batches.add(List.copyOf(invocation.getArgument(0))))
                    .when(deleteImagePort).deleteAllByIdInBatch(anyList());

            service.execute();

            assertThat(batches).hasSize(2);
            assertThat(batches.getFirst()).containsExactlyElementsOf(images.subList(0, 200).stream().map(Image::id).toList());
            assertThat(batches.get(1)).containsExactly(201L);
        }
    }

    private Image image(Long id, String name, LocalDateTime createdAt) {
        return new Image(id, "scheduler/" + name, createdAt);
    }
}
