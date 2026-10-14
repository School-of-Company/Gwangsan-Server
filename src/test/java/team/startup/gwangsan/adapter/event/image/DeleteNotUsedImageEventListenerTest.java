package team.startup.gwangsan.adapter.event.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.application.image.data.DeleteUnusedImagesCommand;
import team.startup.gwangsan.application.image.data.ImageOwnerType;
import team.startup.gwangsan.application.image.port.in.DeleteUnusedImagesUseCase;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteNotUsedImageEventListener 단위 테스트")
class DeleteNotUsedImageEventListenerTest {

    @InjectMocks private DeleteNotUsedImageEventListener listener;
    @Mock private DeleteUnusedImagesUseCase deleteUnusedImagesUseCase;

    @Test
    @DisplayName("이벤트의 각 소유 타입을 그대로 command 로 넘긴다")
    void it_maps_event_to_command() {
        for (ImageOwnerType type : ImageOwnerType.values()) {
            listener.handleDeleteNotUsedImageEvent(new DeleteNotUsedImageEvent(10L, Set.of(20L), type));

            verify(deleteUnusedImagesUseCase).execute(new DeleteUnusedImagesCommand(10L, Set.of(20L), type));
        }
    }

    @Test
    @DisplayName("커밋 후 asyncExecutor 에서 실행된다")
    void it_keeps_after_commit_async_contract() throws NoSuchMethodException {
        var method = DeleteNotUsedImageEventListener.class
                .getMethod("handleDeleteNotUsedImageEvent", DeleteNotUsedImageEvent.class);

        assertThat(method.getAnnotation(Async.class).value()).isEqualTo("asyncExecutor");
        assertThat(method.getAnnotation(TransactionalEventListener.class).phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
