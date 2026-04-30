package team.startup.gwangsan.domain.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.repository.custom.TradeCancelCustomRepository;

import java.util.Optional;

public interface TradeCancelRepository extends JpaRepository<TradeCancel, Long>, TradeCancelCustomRepository {
    boolean existsByTradeCompleteIdAndStatus(Long tradeCompleteId, TradeCancelStatus status);

    Optional<TradeCancel> findByTradeCompleteIdAndStatus(Long tradeCompleteId, TradeCancelStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TradeCancel t SET t.member = :dummy WHERE t.member = :target")
    void reassignMember(@Param("target") Member target, @Param("dummy") Member dummy);
}
