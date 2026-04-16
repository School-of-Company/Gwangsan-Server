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

import java.util.Optional;

public interface TradeCompleteRepository extends JpaRepository<TradeComplete, Long>, TradeCompleteCustomRepository {
    Optional<TradeComplete> findByProductAndSeller(Product product, Member seller);

    boolean existsByProductAndBuyerAndSellerAndStatus(Product product, Member buyer, Member seller, TradeStatus status);

    Optional<TradeComplete> findByProductAndBuyerAndSellerAndStatus(Product product, Member buyer, Member seller, TradeStatus tradeStatus);

    void deleteByProductAndStatus(Product product, TradeStatus tradeStatus);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TradeComplete t SET t.buyer = :dummy WHERE t.buyer = :target")
    void reassignBuyer(@Param("target") Member target, @Param("dummy") Member dummy);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TradeComplete t SET t.seller = :dummy WHERE t.seller = :target")
    void reassignSeller(@Param("target") Member target, @Param("dummy") Member dummy);
}
