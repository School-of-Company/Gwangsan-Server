package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.event.notification.PushRequestedEvent;
import team.startup.gwangsan.application.trade.port.out.TradePushPort;
import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;

/**
 * 거래 완료 푸시. 회원 ID 만 담아 발행하고, notification 이 커밋 후(AFTER_COMMIT) 기기 토큰을 조회해 보낸다.
 * rollback 되면 발송하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class TradePushAdapter implements TradePushPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void notifyCompleted(List<Long> memberIds, long productId) {
        applicationEventPublisher.publishEvent(new PushRequestedEvent(memberIds, NotificationType.TRADE_COMPLETE, productId));
    }
}
