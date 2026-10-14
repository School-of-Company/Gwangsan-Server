package team.startup.gwangsan.adapter.event.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.application.admin.data.CreateAdminAlertCommand;
import team.startup.gwangsan.application.admin.data.CreateAdminAlertEvent;
import team.startup.gwangsan.application.admin.port.in.CreateAdminAlertUseCase;

@Component
@RequiredArgsConstructor
public class CreateAdminAlertEventListener {

    private final CreateAdminAlertUseCase createAdminAlertUseCase;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreateAdminAlertEvent(CreateAdminAlertEvent event) {
        createAdminAlertUseCase.execute(new CreateAdminAlertCommand(event.type(), event.sourceId(), event.memberId()));
    }
}
