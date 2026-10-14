package team.startup.gwangsan.adapter.persistence.trade;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.trade.data.TradeCompleteSummary;
import team.startup.gwangsan.application.trade.port.out.TradeCompletePort;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCompleteException;
import team.startup.gwangsan.domain.trade.model.TradeComplete;
import team.startup.gwangsan.domain.trade.model.TradeRequestState;
import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 상품·회원은 기존 FK association 이라 getReference 로만 연결한다(추가 조회 없음, 같은 영속성 컨텍스트면 캐시된 엔티티).
 */
@Component
@RequiredArgsConstructor
public class TradeCompletePersistenceAdapter implements TradeCompletePort {

    private final TradeCompleteJpaRepository tradeCompleteJpaRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<TradeComplete> findById(long tradeId) {
        return tradeCompleteJpaRepository.findById(tradeId).map(TradeCompletePersistenceAdapter::toDomain);
    }

    @Override
    public Optional<TradeCompleteSummary> findSummary(long tradeId) {
        return tradeCompleteJpaRepository.findById(tradeId)
                .map(t -> new TradeCompleteSummary(t.getId(), t.getProduct().getId(), t.getProduct().getTitle(),
                        t.getBuyer().getId(), t.getBuyer().getNickname(), t.getSeller().getId()));
    }

    @Override
    public Optional<TradeComplete> findPending(long productId, long buyerId, long sellerId) {
        return tradeCompleteJpaRepository.findByProductAndBuyerAndSellerAndStatus(
                        product(productId), member(buyerId), member(sellerId), TradeStatus.PENDING)
                .map(TradeCompletePersistenceAdapter::toDomain);
    }

    @Override
    public Optional<TradeComplete> findCompleted(long productId) {
        return tradeCompleteJpaRepository.findByProductIdAndStatus(productId, TradeStatus.COMPLETED)
                .map(TradeCompletePersistenceAdapter::toDomain);
    }

    @Override
    public List<TradeRequestState> findRequestStates(long productId, long buyerId, long sellerId) {
        return tradeCompleteJpaRepository.findTradeState(product(productId), member(buyerId), member(sellerId)).stream()
                .map(s -> new TradeRequestState(s.getStatus(), s.getRequestedBySeller(), s.getCreatedAt()))
                .toList();
    }

    @Override
    public TradeComplete create(TradeComplete tradeComplete) {
        TradeCompleteJpaEntity entity = TradeCompleteJpaEntity.builder()
                .product(product(tradeComplete.productId()))
                .buyer(member(tradeComplete.buyerId()))
                .seller(member(tradeComplete.sellerId()))
                .status(tradeComplete.status())
                .requestedBySeller(tradeComplete.requestedBySeller())
                .build();
        if (tradeComplete.completedAt() != null) {
            entity.updateCompletedAt(tradeComplete.completedAt());
        }
        return toDomain(tradeCompleteJpaRepository.save(entity));
    }

    @Override
    public void update(TradeComplete tradeComplete) {
        TradeCompleteJpaEntity entity = tradeCompleteJpaRepository.findById(tradeComplete.id())
                .orElseThrow(NotFoundTradeCompleteException::new);
        entity.updateStatus(tradeComplete.status());
        if (!Objects.equals(entity.getCompletedAt(), tradeComplete.completedAt())) {
            entity.updateCompletedAt(tradeComplete.completedAt());
        }
    }

    @Override
    public void delete(long tradeId) {
        tradeCompleteJpaRepository.findById(tradeId).ifPresent(tradeCompleteJpaRepository::delete);
    }

    @Override
    public void deleteOtherPending(long productId) {
        tradeCompleteJpaRepository.deleteByProductAndStatus(product(productId), TradeStatus.PENDING);
    }

    static TradeComplete toDomain(TradeCompleteJpaEntity e) {
        return new TradeComplete(e.getId(), e.getProduct().getId(), e.getBuyer().getId(), e.getSeller().getId(),
                e.getStatus(), e.isRequestedBySeller(), e.getCreatedAt(), e.getCompletedAt());
    }

    private Product product(long productId) {
        return entityManager.getReference(Product.class, productId);
    }

    private MemberJpaEntity member(long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }
}
