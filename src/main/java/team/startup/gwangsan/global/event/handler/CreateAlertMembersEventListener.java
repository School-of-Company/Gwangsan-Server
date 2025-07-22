package team.startup.gwangsan.global.event.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.domain.alert.service.CreateAlertMembersService;
import team.startup.gwangsan.global.event.CreateAlertMembersEvent;

@Component
@RequiredArgsConstructor
public class CreateAlertMembersEventListener {

    private final CreateAlertMembersService createAlertMembersService;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreateAlertMembersEvent(CreateAlertMembersEvent event) {
        createAlertMembersService.execute(event.sourceId(), event.memberIds(), event.alertType());
    }
}
