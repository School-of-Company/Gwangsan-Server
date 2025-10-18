package team.startup.gwangsan.domain.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.repository.custom.TradeCancelCustomRepository;

import java.util.Optional;

public interface TradeCancelRepository extends JpaRepository<TradeCancel, Long>, TradeCancelCustomRepository {
    boolean existsByTradeCompleteIdAndStatus(Long tradeCompleteId, TradeCancelStatus status);

    Optional<TradeCancel> findByTradeCompleteIdAndStatus(Long tradeCompleteId, TradeCancelStatus status);
}
