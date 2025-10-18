package team.startup.gwangsan.domain.trade.repository.custom;

import team.startup.gwangsan.domain.trade.entity.TradeCancelImage;

import java.util.List;

public interface TradeCancelImageCustomRepository {
    List<TradeCancelImage> findByTradeCancelIn(List<Long> tradeCancelIds);
}
