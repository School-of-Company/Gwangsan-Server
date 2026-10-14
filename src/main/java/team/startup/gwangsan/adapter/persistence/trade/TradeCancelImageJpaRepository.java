package team.startup.gwangsan.adapter.persistence.trade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeCancelImageJpaRepository extends JpaRepository<TradeCancelImageJpaEntity, Long>, TradeCancelImageCustomRepository {
    void deleteByTradeCancelId(Long tradeCancelId);
}
