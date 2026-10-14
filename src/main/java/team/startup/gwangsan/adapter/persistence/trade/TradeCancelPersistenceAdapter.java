package team.startup.gwangsan.adapter.persistence.trade;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.trade.port.out.TradeCancelPort;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.model.TradeCancel;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TradeCancelPersistenceAdapter implements TradeCancelPort {

    private final TradeCancelJpaRepository tradeCancelJpaRepository;
    private final TradeCancelImageJpaRepository tradeCancelImageJpaRepository;
    // ponytail: 이미지 엔티티 조회는 PR05 image 공개 조회 전까지 기존 Repository 를 그대로 쓴다.
    private final ImageRepository imageRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<TradeCancel> findPendingByTrade(long tradeCompleteId) {
        return tradeCancelJpaRepository.findByTradeCompleteIdAndStatus(tradeCompleteId, TradeCancelStatus.PENDING)
                .map(TradeCancelPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Long> findProductId(long tradeCancelId) {
        return tradeCancelJpaRepository.findProductIdById(tradeCancelId);
    }

    @Override
    public Optional<TradeCancel> findById(long tradeCancelId) {
        return tradeCancelJpaRepository.findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(tradeCancelId)
                .map(TradeCancelPersistenceAdapter::toDomain);
    }

    @Override
    public Map<Long, Long> findProductIds(Collection<Long> tradeCancelIds) {
        return tradeCancelJpaRepository.findProductIdsByIdIn(tradeCancelIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    @Override
    public TradeCancel create(TradeCancel tradeCancel, List<Long> imageIds) {
        TradeCancelJpaEntity saved = tradeCancelJpaRepository.save(TradeCancelJpaEntity.builder()
                .tradeComplete(entityManager.getReference(TradeCompleteJpaEntity.class, tradeCancel.tradeCompleteId()))
                .member(entityManager.getReference(MemberJpaEntity.class, tradeCancel.requesterId()))
                .reason(tradeCancel.reason())
                .status(tradeCancel.status())
                .build());

        List<Image> images = imageRepository.findAllById(imageIds);
        tradeCancelImageJpaRepository.saveAll(images.stream()
                .map(image -> TradeCancelImageJpaEntity.builder().tradeCancel(saved).image(image).build())
                .toList());

        return toDomain(saved);
    }

    @Override
    public void update(TradeCancel tradeCancel) {
        tradeCancelJpaRepository.findById(tradeCancel.id())
                .orElseThrow(NotFoundTradeCancelException::new)
                .updateStatus(tradeCancel.status());
    }

    @Override
    public void deleteImages(long tradeCancelId) {
        tradeCancelImageJpaRepository.deleteByTradeCancelId(tradeCancelId);
    }

    private static TradeCancel toDomain(TradeCancelJpaEntity e) {
        return new TradeCancel(e.getId(), e.getTradeComplete().getId(), e.getMember().getId(), e.getReason(), e.getStatus());
    }
}
