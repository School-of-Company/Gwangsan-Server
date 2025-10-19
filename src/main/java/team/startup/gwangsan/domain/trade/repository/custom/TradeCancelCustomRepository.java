package team.startup.gwangsan.domain.trade.repository.custom;

import team.startup.gwangsan.domain.trade.entity.TradeCancel;

import java.util.List;
import java.util.Optional;

public interface TradeCancelCustomRepository {
    List<TradeCancel> findAllByIdIn(List<Long> ids);

    Optional<TradeCancel> findByIdWithTradeCompleteAndMember(Long id);

    Optional<TradeCancel> findByIdWithMember(Long id);

    Optional<TradeCancel> findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(Long id);
}
