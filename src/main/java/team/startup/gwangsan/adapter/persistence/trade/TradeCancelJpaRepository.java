package team.startup.gwangsan.adapter.persistence.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TradeCancelJpaRepository extends JpaRepository<TradeCancelJpaEntity, Long>, TradeCancelCustomRepository {
    Optional<TradeCancelJpaEntity> findByTradeCompleteIdAndStatus(Long tradeCompleteId, TradeCancelStatus status);

    /**
     * 상품 ID 만 스칼라로 읽는다. 엔티티를 영속화하지 않아 1차 캐시를 오염시키지 않으므로,
     * 잠금을 잡기 전에 잠글 대상을 알아내는 용도로만 쓴다.
     */
    @Query("select tc.tradeComplete.product.id from TradeCancelJpaEntity tc where tc.id = :id")
    Optional<Long> findProductIdById(@Param("id") Long id);

    @Query("select tc.id, tc.tradeComplete.product.id from TradeCancelJpaEntity tc where tc.id in :ids")
    List<Object[]> findProductIdsByIdIn(@Param("ids") Collection<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TradeCancelJpaEntity t SET t.member = :dummy WHERE t.member = :target")
    void reassignMember(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
