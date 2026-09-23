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
    Optional<TradeCancel> findByTradeCompleteIdAndStatus(Long tradeCompleteId, TradeCancelStatus status);

    /**
     * 상품 ID 만 스칼라로 읽는다. 엔티티를 영속화하지 않아 1차 캐시를 오염시키지 않으므로,
     * 잠금을 잡기 전에 잠글 대상을 알아내는 용도로만 쓴다.
     */
    @Query("select tc.tradeComplete.product.id from TradeCancel tc where tc.id = :id")
    Optional<Long> findProductIdById(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TradeCancel t SET t.member = :dummy WHERE t.member = :target")
    void reassignMember(@Param("target") Member target, @Param("dummy") Member dummy);
}
