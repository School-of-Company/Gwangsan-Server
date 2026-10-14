package team.startup.gwangsan.adapter.persistence.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("ProductRepository 통합 테스트")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProductRepository productRepository;

    private MemberJpaEntity member;

    @BeforeEach
    void setUp() {
        member = em.persist(MemberJpaEntity.builder()
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
    @DisplayName("관리자 목록은")
    class Describe_findAdminProducts {
        @ParameterizedTest
        @CsvSource(value = {"NULL,NULL,4", "OBJECT,NULL,2", "SERVICE,NULL,2", "NULL,GIVER,2", "NULL,RECEIVER,2", "OBJECT,GIVER,1", "OBJECT,RECEIVER,1", "SERVICE,GIVER,1", "SERVICE,RECEIVER,1"}, nullValues = "NULL")
        @DisplayName("선택 필터를 각각 및 조합 적용하고 삭제 상태를 제외한다")
        void it_applies_optional_filters(Type type, Mode mode, int count) {
            for (Type productType : Type.values()) {
                for (Mode productMode : Mode.values()) {
                    em.persist(Product.builder().title("게시글").description("설명").gwangsan(5000)
                            .member(member).status(ProductStatus.ONGOING).type(productType).mode(productMode).build());
                }
            }
            persistProduct(ProductStatus.DELETED);
            em.flush();
            em.clear();

            var result = productRepository.findAdminProducts(type, mode, null, 20);

            assertThat(result).hasSize(count);
            assertThat(result).allSatisfy(product -> {
                if (type != null) assertThat(product.getType()).isEqualTo(type);
                if (mode != null) assertThat(product.getMode()).isEqualTo(mode);
                assertThat(product.getStatus()).isNotEqualTo(ProductStatus.DELETED);
            });
        }

        @Test
        @DisplayName("진행중·예약·완료 게시글을 ID 역순으로 중복 없이 페이지 조회한다")
        void it_pages_all_active_statuses_with_exclusive_cursor() {
            Product first = persistProduct(ProductStatus.ONGOING);
            Product second = persistProduct(ProductStatus.RESERVATION);
            Product third = persistProduct(ProductStatus.COMPLETED);
            Product deleted = persistProduct(ProductStatus.DELETED);
            em.flush();
            em.clear();

            assertThat(productRepository.findAdminProducts(null, null, null, 2))
                    .extracting(Product::getId).containsExactly(third.getId(), second.getId());
            assertThat(productRepository.findAdminProducts(null, null, second.getId(), 2))
                    .extracting(Product::getId).containsExactly(first.getId());
            assertThat(productRepository.findAdminProducts(null, null, first.getId(), 2)).isEmpty();
            assertThat(productRepository.findAdminProducts(null, null, deleted.getId(), 1))
                    .extracting(Product::getId).containsExactly(third.getId());
            assertThat(productRepository.findAdminProducts(Type.OBJECT, null, null, 2)).isEmpty();
        }
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
