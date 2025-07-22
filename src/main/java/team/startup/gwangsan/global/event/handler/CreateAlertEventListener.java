package team.startup.gwangsan.global.event.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.domain.alert.service.CreateAlertService;
import team.startup.gwangsan.global.event.CreateAlertEvent;

@Component
@RequiredArgsConstructor
public class CreateAlertEventListener {

    private final CreateAlertService createAlertService;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreateAlertEvent(CreateAlertEvent event) {
        createAlertService.execute(event.sourceId(), event.memberId(), event.alertType());
    }
}
