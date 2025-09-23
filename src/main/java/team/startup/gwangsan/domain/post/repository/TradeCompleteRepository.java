package team.startup.gwangsan.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.TradeComplete;
import team.startup.gwangsan.domain.post.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.post.repository.custom.TradeCompleteCustomRepository;

import java.util.Optional;

public interface TradeCompleteRepository extends JpaRepository<TradeComplete, Long>, TradeCompleteCustomRepository {
    Optional<TradeComplete> findByProductAndSeller(Product product, Member seller);

    boolean existsByProductAndBuyerAndSellerAndStatus(Product product, Member buyer, Member seller, TradeStatus status);

    Optional<TradeComplete> findByProductAndBuyerAndSellerAndStatus(Product product, Member buyer, Member seller, TradeStatus tradeStatus);
}
