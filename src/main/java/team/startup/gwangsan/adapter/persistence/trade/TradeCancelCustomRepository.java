package team.startup.gwangsan.adapter.persistence.trade;


import java.util.List;
import java.util.Optional;

public interface TradeCancelCustomRepository {
    List<TradeCancelJpaEntity> findAllByIdIn(List<Long> ids);

    Optional<TradeCancelJpaEntity> findByIdWithTradeCompleteAndMember(Long id);

    Optional<TradeCancelJpaEntity> findByIdWithMember(Long id);

    Optional<TradeCancelJpaEntity> findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(Long id);
}
