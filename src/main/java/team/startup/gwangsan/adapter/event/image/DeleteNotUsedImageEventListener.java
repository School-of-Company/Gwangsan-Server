package team.startup.gwangsan.adapter.event.image;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.application.image.data.DeleteUnusedImagesCommand;
import team.startup.gwangsan.application.image.data.ImageOwnerType;
import team.startup.gwangsan.application.image.port.in.DeleteUnusedImagesUseCase;
import team.startup.gwangsan.global.event.DeleteNotUsedImageEvent;

// 이벤트 record 는 발행자(post UpdateProductServiceImpl)가 import 하므로 PR06 까지 global 에 둔다.
@Component
@RequiredArgsConstructor
public class DeleteNotUsedImageEventListener {

    private final DeleteUnusedImagesUseCase deleteUnusedImagesUseCase;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeleteNotUsedImageEvent(DeleteNotUsedImageEvent event) {
        deleteUnusedImagesUseCase.execute(new DeleteUnusedImagesCommand(
                event.sourceId(), event.imageIds(), ImageOwnerType.valueOf(event.type().name())));
    }
}
