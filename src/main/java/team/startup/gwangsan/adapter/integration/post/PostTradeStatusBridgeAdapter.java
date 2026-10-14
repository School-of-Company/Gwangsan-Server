package team.startup.gwangsan.adapter.integration.post;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.port.out.TradeStatusPort;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.trade.service.TradeStateReader;
import team.startup.gwangsan.domain.trade.service.TradeStateSnapshot;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

/**
 * trade 업무(PR07) 미전환 기간의 bridge. TradeStateReader 가 상품 Entity 의 상태를 읽으므로
 * 상품 저장(ProductPort.save) 이후에 호출해야 한다. PR07 의 공개 계약으로 교체한다.
 */
@Component
@RequiredArgsConstructor
public class PostTradeStatusBridgeAdapter implements TradeStatusPort {

    private final TradeStateReader tradeStateReader;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Override
    public void publishChanged(Long roomId, Long productId, Long buyerId, Long sellerId) {
        TradeStateSnapshot state = tradeStateReader.read(entityManager.find(Product.class, productId),
                entityManager.getReference(MemberJpaEntity.class, buyerId), entityManager.getReference(MemberJpaEntity.class, sellerId));
        eventPublisher.publishEvent(new TradeStatusChangedEvent(roomId, productId, state.completed(), state.reserved(),
                state.requestedBySeller(), state.requestedAt()));
    }
}
