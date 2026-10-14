package team.startup.gwangsan.adapter.event.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.application.notification.port.in.SendPushUseCase;
import team.startup.gwangsan.domain.notification.model.PushMessage;

@Component
@RequiredArgsConstructor
public class PushRequestedEventListener {

    private final SendPushUseCase sendPushUseCase;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PushRequestedEvent event) {
        sendPushUseCase.send(new SendPushCommand(event.recipientMemberIds(), PushMessage.byType(event.type(), event.sourceId())));
    }
}
