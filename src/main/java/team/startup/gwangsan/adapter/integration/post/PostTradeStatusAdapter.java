package team.startup.gwangsan.adapter.integration.post;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.event.trade.TradeStatusChangedEvent;
import team.startup.gwangsan.application.post.port.out.TradeStatusPort;
import team.startup.gwangsan.application.trade.data.TradeStateKey;
import team.startup.gwangsan.application.trade.data.TradeStateSnapshot;
import team.startup.gwangsan.application.trade.port.in.TradeStateQuery;

/**
 * post → trade 공개 query 연결. 거래 상태는 trade 의 단일 계산(TradeStateQuery)을 쓰고,
 * 기존 TradeStatusChangedEvent(커밋 후 비동기 전달)로 발행한다. 상품 상태 저장 이후에 호출한다.
 */
@Component
@RequiredArgsConstructor
public class PostTradeStatusAdapter implements TradeStatusPort {

    private final TradeStateQuery tradeStateQuery;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishChanged(Long roomId, Long productId, Long buyerId, Long sellerId) {
        TradeStateSnapshot state = tradeStateQuery.query(new TradeStateKey(productId, buyerId, sellerId));
        eventPublisher.publishEvent(new TradeStatusChangedEvent(roomId, productId, state.completed(), state.reserved(),
                state.requestedBySeller(), state.requestedAt()));
    }
}
