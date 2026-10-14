package team.startup.gwangsan.adapter.integration.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEvent;
import team.startup.gwangsan.application.chat.port.out.ChatPushPort;
import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;

/**
 * chat → notification. 저장 트랜잭션 안에서 회원 ID 로 푸시를 요청하고, 기기 토큰 조회와 발송은 커밋 후 notification 이 한다.
 */
@Component
@RequiredArgsConstructor
public class ChatPushBridge implements ChatPushPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void notifyNewMessage(Long recipientId, Long roomId) {
        eventPublisher.publishEvent(new PushRequestedEvent(List.of(recipientId), NotificationType.CHATTING, roomId));
    }
}
