package team.startup.gwangsan.adapter.event.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.trade.data.TradeStatusChange;
import team.startup.gwangsan.application.trade.port.out.TradeStatusPort;

/** 거래 상태 변경을 기존 TradeStatusChangedEvent(커밋 후 비동기 HTTP 전달)로 발행한다. */
@Component
@RequiredArgsConstructor
public class TradeStatusEventPublisher implements TradeStatusPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(TradeStatusChange change) {
        applicationEventPublisher.publishEvent(new TradeStatusChangedEvent(change.roomId(), change.productId(),
                change.completed(), change.reserved(), change.requestedBySeller(), change.requestedAt()));
    }
}
