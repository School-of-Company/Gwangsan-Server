package team.startup.gwangsan.global.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.global.chat.notification.ChattingServerMessageMutationNotifier;
import team.startup.gwangsan.global.event.ChatMessageMutationEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageMutationEventListener {
    private final ChattingServerMessageMutationNotifier notifier;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageMutationEvent event) {
        try {
            notifier.notifyMutation(event);
        } catch (Exception e) {
            log.error("[CHAT-MUTATION] notification failed. messageId={}, kind={}",
                    event.messageId(), event.kind(), e);
        }
    }
}
