package team.startup.gwangsan.adapter.event.image;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.application.image.data.DeleteUnusedImagesCommand;
import team.startup.gwangsan.application.image.port.in.DeleteUnusedImagesUseCase;

@Component
@RequiredArgsConstructor
public class DeleteNotUsedImageEventListener {

    private final DeleteUnusedImagesUseCase deleteUnusedImagesUseCase;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeleteNotUsedImageEvent(DeleteNotUsedImageEvent event) {
        deleteUnusedImagesUseCase.execute(new DeleteUnusedImagesCommand(
                event.sourceId(), event.imageIds(), event.type()));
    }
}
