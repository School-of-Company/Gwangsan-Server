package team.startup.gwangsan.domain.post.repository;

import org.junit.jupiter.api.BeforeEach;
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
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("ProductRepository 통합 테스트")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProductRepository productRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = em.persist(Member.builder()
                .name("홍길동")
                .nickname("길동")
                .phoneNumber("01000000000")
                .password("password")
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Product persistProduct(ProductStatus status) {
        return em.persist(Product.builder()
                .title("제목")
                .description("설명")
                .gwangsan(5000)
                .member(member)
                .status(status)
                .type(Type.SERVICE)
                .mode(Mode.GIVER)
                .build());
    }

    @Nested
    @DisplayName("findActiveById 메서드는")
    class Describe_findActiveById {

        @Test
        @DisplayName("삭제되지 않은 게시글을 반환한다")
        void it_returns_product_when_not_deleted() {
            Product product = persistProduct(ProductStatus.ONGOING);
            em.flush();
            em.clear();

            assertThat(productRepository.findActiveById(product.getId())).isPresent();
        }

        @Test
        @DisplayName("DELETED 상태의 게시글은 반환하지 않는다")
        void it_returns_empty_when_deleted() {
            Product product = persistProduct(ProductStatus.DELETED);
            em.flush();
            em.clear();

            assertThat(productRepository.findActiveById(product.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdWithLock 메서드는")
    class Describe_findByIdWithLock {

        @Test
        @DisplayName("DELETED 상태의 게시글은 반환하지 않는다")
        void it_returns_empty_when_deleted() {
            Product product = persistProduct(ProductStatus.DELETED);
            em.flush();
            em.clear();

            assertThat(productRepository.findByIdWithLock(product.getId())).isEmpty();
        }
    }
}
