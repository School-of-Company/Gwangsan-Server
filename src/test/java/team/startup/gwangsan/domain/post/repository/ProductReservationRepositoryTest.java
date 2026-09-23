package team.startup.gwangsan.domain.post.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("ProductReservationRepository 통합 테스트")
class ProductReservationRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProductReservationRepository productReservationRepository;

    private Member createMember(String nickname, String phone) {
        return em.persist(Member.builder()
                .name("테스트")
                .nickname(nickname)
                .password("pw")
                .phoneNumber(phone)
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Product createProduct(Member owner) {
        return em.persist(Product.builder()
                .title("상품")
                .description("설명")
                .gwangsan(5000)
                .member(owner)
                .type(Type.SERVICE)
                .mode(Mode.GIVER)
                .status(ProductStatus.ONGOING)
                .build());
    }

    private ProductReservation reserveAndCancel(Product product, Member reserver) {
        ProductReservation reservation = productReservationRepository.save(ProductReservation.builder()
                .product(product)
                .reserver(reserver)
                .status(ReservationStatus.PENDING)
                .build());
        reservation.cancel();
        em.flush();
        return reservation;
    }

    @Nested
    @DisplayName("같은 상품의 예약을 반복 취소할 때")
    class Describe_repeated_cancel {

        @Test
        @DisplayName("취소 이력이 여러 건 쌓여도 저장에 실패하지 않는다")
        void it_saves_multiple_cancelled_reservations_for_same_product() {
            Member seller = createMember("seller", "010-0001-0001");
            Member reserver = createMember("reserver", "010-0001-0002");
            Product product = createProduct(seller);

            reserveAndCancel(product, reserver);

            assertThatCode(() -> reserveAndCancel(product, reserver)).doesNotThrowAnyException();

            em.clear();
            List<ProductReservation> reservations = productReservationRepository.findAll();
            assertThat(reservations)
                    .hasSize(2)
                    .allMatch(reservation -> reservation.getStatus() == ReservationStatus.CANCELLED);
        }
    }
}
