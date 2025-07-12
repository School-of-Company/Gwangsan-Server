package team.startup.gwangsan.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.TradeComplete;
import team.startup.gwangsan.domain.post.repository.custom.TradeCompleteCustomRepository;

public interface TradeCompleteRepository extends JpaRepository<TradeComplete, Long>, TradeCompleteCustomRepository {
    boolean existsByProductAndMemberAndOtherMember(Product product, Member member, Member otherMember);
}
