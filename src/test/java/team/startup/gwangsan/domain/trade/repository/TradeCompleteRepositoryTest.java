package team.startup.gwangsan.domain.trade.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository.TradeStateProjection;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
class TradeCompleteRepositoryTest {

    @Autowired
    private TradeCompleteRepository repository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    @Test
    void findsPendingAndCompletedStateWithOneProjectionQuery() {
        Member buyer = member("buyer", "010-1000-0001");
        Member seller = member("seller", "010-1000-0002");
        Product product = entityManager.persist(Product.builder()
                .title("상품")
                .description("설명")
                .gwangsan(1000)
                .member(seller)
                .type(Type.SERVICE)
                .mode(Mode.GIVER)
                .status(ProductStatus.ONGOING)
                .build());

        entityManager.persist(TradeComplete.builder()
                .product(product).buyer(buyer).seller(seller)
                .status(TradeStatus.PENDING).requestedBySeller(true).build());
        entityManager.persist(TradeComplete.builder()
                .product(product).buyer(buyer).seller(seller)
                .status(TradeStatus.COMPLETED).requestedBySeller(false).build());
        entityManager.flush();
        entityManager.clear();

        List<TradeStateProjection> states = repository.findTradeState(product, buyer, seller);

        assertThat(states).extracting(TradeStateProjection::getStatus)
                .containsExactlyInAnyOrder(TradeStatus.PENDING, TradeStatus.COMPLETED);
        assertThat(states).allSatisfy(state -> assertThat(state.getCreatedAt()).isNotNull());
    }

    private Member member(String nickname, String phoneNumber) {
        return entityManager.persist(Member.builder()
                .name(nickname)
                .nickname(nickname)
                .password("pw")
                .phoneNumber(phoneNumber)
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }
}
