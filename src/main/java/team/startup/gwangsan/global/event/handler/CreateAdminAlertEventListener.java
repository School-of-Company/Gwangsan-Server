package team.startup.gwangsan.global.event.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.domain.admin.service.CreateAdminAlertService;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;

@Component
@RequiredArgsConstructor
public class CreateAdminAlertEventListener {

    private final CreateAdminAlertService createAdminAlertService;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreateAdminAlertEvent(CreateAdminAlertEvent event) {
        createAdminAlertService.execute(event.type(), event.sourceId(), event.member());
    }
}
