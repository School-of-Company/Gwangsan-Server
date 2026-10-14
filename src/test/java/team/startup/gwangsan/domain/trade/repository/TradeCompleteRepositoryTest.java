package team.startup.gwangsan.domain.trade.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
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
        MemberJpaEntity buyer = member("buyer", "010-1000-0001");
        MemberJpaEntity seller = member("seller", "010-1000-0002");
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

    private MemberJpaEntity member(String nickname, String phoneNumber) {
        return entityManager.persist(MemberJpaEntity.builder()
                .name(nickname)
                .nickname(nickname)
                .password("pw")
                .phoneNumber(phoneNumber)
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }
}
