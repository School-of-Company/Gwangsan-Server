package team.startup.gwangsan.domain.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancelImage;
import team.startup.gwangsan.domain.trade.repository.custom.TradeCancelImageCustomRepository;

import java.util.List;

public interface TradeCancelImageRepository extends JpaRepository<TradeCancelImage, Long>, TradeCancelImageCustomRepository {
    void deleteByTradeCancelId(Long tradeCancelId);
}
