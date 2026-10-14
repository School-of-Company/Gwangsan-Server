package team.startup.gwangsan.adapter.persistence.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeCompleteJpaRepository extends JpaRepository<TradeCompleteJpaEntity, Long>, TradeCompleteCustomRepository {
    interface TradeStateProjection {
        TradeStatus getStatus();
        Boolean getRequestedBySeller();
        LocalDateTime getCreatedAt();
    }

    Optional<TradeCompleteJpaEntity> findByProductAndBuyerAndSellerAndStatus(Product product, MemberJpaEntity buyer, MemberJpaEntity seller, TradeStatus tradeStatus);

    @Query("""
            SELECT t.status AS status, t.requestedBySeller AS requestedBySeller, t.createdAt AS createdAt
            FROM TradeCompleteJpaEntity t
            WHERE t.product = :product AND t.buyer = :buyer AND t.seller = :seller
              AND t.status IN (team.startup.gwangsan.domain.trade.model.TradeStatus.PENDING,
                               team.startup.gwangsan.domain.trade.model.TradeStatus.COMPLETED)
            """)
    List<TradeStateProjection> findTradeState(@Param("product") Product product,
                                               @Param("buyer") MemberJpaEntity buyer,
                                               @Param("seller") MemberJpaEntity seller);

    Optional<TradeCompleteJpaEntity> findByProductAndStatus(Product product, TradeStatus tradeStatus);

    void deleteByProductAndStatus(Product product, TradeStatus tradeStatus);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TradeCompleteJpaEntity t SET t.buyer = :dummy WHERE t.buyer = :target")
    void reassignBuyer(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TradeCompleteJpaEntity t SET t.seller = :dummy WHERE t.seller = :target")
    void reassignSeller(@Param("target") MemberJpaEntity target, @Param("dummy") MemberJpaEntity dummy);
}
