package team.startup.gwangsan.adapter.persistence.trade;


import java.util.List;

public interface TradeCancelImageCustomRepository {
    List<TradeCancelImageJpaEntity> findByTradeCancelIn(List<Long> tradeCancelIds);
}
