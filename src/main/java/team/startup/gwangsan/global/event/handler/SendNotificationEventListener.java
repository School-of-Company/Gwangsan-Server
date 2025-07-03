package team.startup.gwangsan.global.event.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.domain.notification.service.SendNotificationService;
import team.startup.gwangsan.global.event.SendNotificationEvent;

@Component
@RequiredArgsConstructor
public class SendNotificationEventListener {

    private final SendNotificationService sendNotificationService;

    @Async
    @TransactionalEventListener(value = SendNotificationEvent.class, phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotification(SendNotificationEvent event) {
        sendNotificationService.execute(event.deviceTokens(), event.type());
    }
}
