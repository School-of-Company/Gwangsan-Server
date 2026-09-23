package team.startup.gwangsan.domain.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.repository.custom.TradeCompleteCustomRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeCompleteRepository extends JpaRepository<TradeComplete, Long>, TradeCompleteCustomRepository {
    interface TradeStateProjection {
        TradeStatus getStatus();
        Boolean getRequestedBySeller();
        LocalDateTime getCreatedAt();
    }

    Optional<TradeComplete> findByProductAndBuyerAndSellerAndStatus(Product product, Member buyer, Member seller, TradeStatus tradeStatus);

    @Query("""
            SELECT t.status AS status, t.requestedBySeller AS requestedBySeller, t.createdAt AS createdAt
            FROM TradeComplete t
            WHERE t.product = :product AND t.buyer = :buyer AND t.seller = :seller
              AND t.status IN (team.startup.gwangsan.domain.trade.entity.constant.TradeStatus.PENDING,
                               team.startup.gwangsan.domain.trade.entity.constant.TradeStatus.COMPLETED)
            """)
    List<TradeStateProjection> findTradeState(@Param("product") Product product,
                                               @Param("buyer") Member buyer,
                                               @Param("seller") Member seller);

    Optional<TradeComplete> findByProductAndStatus(Product product, TradeStatus tradeStatus);

    void deleteByProductAndStatus(Product product, TradeStatus tradeStatus);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TradeComplete t SET t.buyer = :dummy WHERE t.buyer = :target")
    void reassignBuyer(@Param("target") Member target, @Param("dummy") Member dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TradeComplete t SET t.seller = :dummy WHERE t.seller = :target")
    void reassignSeller(@Param("target") Member target, @Param("dummy") Member dummy);
}
