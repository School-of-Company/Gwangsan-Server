package team.startup.gwangsan.global.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.global.chat.notification.ChattingServerTradeStatusNotifier;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeStatusChangedEventListener {

    private final ChattingServerTradeStatusNotifier notifier;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTradeStatusChangedEvent(TradeStatusChangedEvent event) {
        try {
            notifier.notifyTradeStatusChanged(event);
        } catch (Exception e) {
            log.warn(
                    "Failed to notify chatting server of trade status change. roomId={}, productId={}, completed={}",
                    event.roomId(),
                    event.productId(),
                    event.completed(),
                    e
            );
        }
    }
}
