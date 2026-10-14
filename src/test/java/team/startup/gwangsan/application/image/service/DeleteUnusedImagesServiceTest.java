package team.startup.gwangsan.application.image.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.image.data.DeleteUnusedImagesCommand;
import team.startup.gwangsan.application.image.data.ImageOwnerType;
import team.startup.gwangsan.application.image.port.out.DeleteImagePort;
import team.startup.gwangsan.application.image.port.out.ImageReferenceCleanupPort;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteUnusedImagesService 단위 테스트")
class DeleteUnusedImagesServiceTest {

    @InjectMocks private DeleteUnusedImagesService service;
    @Mock private ImageReferenceCleanupPort imageReferenceCleanupPort;
    @Mock private DeleteImagePort deleteImagePort;

    @Test
    @DisplayName("이미지마다 참조 해제 후 이미지를 삭제한다")
    void it_unlinks_then_deletes_each_image() {
        service.execute(new DeleteUnusedImagesCommand(10L, new LinkedHashSet<>(List.of(1L, 2L)), ImageOwnerType.PRODUCT));

        InOrder order = inOrder(imageReferenceCleanupPort, deleteImagePort);
        order.verify(imageReferenceCleanupPort).unlink(ImageOwnerType.PRODUCT, 10L, 1L);
        order.verify(deleteImagePort).deleteById(1L);
        order.verify(imageReferenceCleanupPort).unlink(ImageOwnerType.PRODUCT, 10L, 2L);
        order.verify(deleteImagePort).deleteById(2L);
    }

    @Test
    @DisplayName("이미지 ID 가 비어 있으면 아무것도 호출하지 않는다")
    void it_does_nothing_for_empty_ids() {
        service.execute(new DeleteUnusedImagesCommand(10L, Set.of(), ImageOwnerType.NOTICE));

        verifyNoInteractions(imageReferenceCleanupPort, deleteImagePort);
    }
}
