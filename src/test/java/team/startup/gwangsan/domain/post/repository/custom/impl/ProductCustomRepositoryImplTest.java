package team.startup.gwangsan.domain.post.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("ProductCustomRepositoryImpl H2 슬라이스 통합 테스트")
class ProductCustomRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JPAQueryFactory queryFactory;

    private ProductCustomRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ProductCustomRepositoryImpl(queryFactory);
    }

    @Test
    @DisplayName("관리자 조회는 null 선택 필터에서 삭제 글을 숨기고 ID 내림차순 커서를 적용한다")
    void it_filters_admin_products_and_uses_an_exclusive_cursor() {
        Member owner = member("owner");
        Product oldest = product(owner, Type.OBJECT, Mode.GIVER, ProductStatus.ONGOING);
        Product cursor = product(owner, Type.OBJECT, Mode.RECEIVER, ProductStatus.RESERVATION);
        Product newest = product(owner, Type.SERVICE, Mode.RECEIVER, ProductStatus.COMPLETED);
        product(owner, Type.SERVICE, Mode.GIVER, ProductStatus.DELETED);
        em.flush();
        em.clear();

        List<Product> firstPage = repository.findAdminProducts(null, null, null, 2);

        assertThat(firstPage).extracting(Product::getId).containsExactly(newest.getId(), cursor.getId());
        assertThat(firstPage).allSatisfy(found -> assertThat(found.getStatus()).isNotEqualTo(ProductStatus.DELETED));
        assertThat(isLoaded(firstPage.getFirst().getMember())).isTrue();
        assertThat(repository.findAdminProducts(Type.OBJECT, Mode.RECEIVER, null, 5))
                .extracting(Product::getId).containsExactly(cursor.getId());
        assertThat(repository.findAdminProducts(null, null, cursor.getId(), 5))
                .extracting(Product::getId).containsExactly(oldest.getId());
    }

    @Test
    @DisplayName("동네 목록은 type·mode·상태와 회원의 지점을 함께 필터링하고 null 선택 필터를 생략한다")
    void it_filters_products_by_member_place_and_optional_product_fields() {
        Place targetPlace = place("대상");
        Place otherPlace = place("다른");
        Product matching = product(memberWithDetail("matching", targetPlace), Type.SERVICE, Mode.GIVER, ProductStatus.ONGOING);
        Product differentMode = product(memberWithDetail("mode", targetPlace), Type.SERVICE, Mode.RECEIVER, ProductStatus.ONGOING);
        Product differentStatus = product(memberWithDetail("status", targetPlace), Type.SERVICE, Mode.GIVER, ProductStatus.COMPLETED);
        product(memberWithDetail("place", otherPlace), Type.SERVICE, Mode.GIVER, ProductStatus.ONGOING);
        em.flush();
        em.clear();

        List<Product> filtered = repository.findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(
                Type.SERVICE, Mode.GIVER, targetPlace, ProductStatus.ONGOING);

        assertThat(filtered).extracting(Product::getId).containsExactly(matching.getId());
        assertThat(isLoaded(filtered.getFirst().getMember())).isTrue();
        assertThat(repository.findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(null, null, targetPlace, null))
                .extracting(Product::getId)
                .containsExactlyInAnyOrder(matching.getId(), differentMode.getId(), differentStatus.getId());
    }

    @Test
    @DisplayName("내 글 조회는 회원을 고정하고 null 또는 빈 상태 목록을 생략하며 전달한 상태 목록만 제한한다")
    void it_filters_member_products_by_optional_fields_and_statuses() {
        Member owner = member("owner");
        Member other = member("other");
        Product ongoing = product(owner, Type.SERVICE, Mode.GIVER, ProductStatus.ONGOING);
        Product reservation = product(owner, Type.SERVICE, Mode.GIVER, ProductStatus.RESERVATION);
        Product completed = product(owner, Type.OBJECT, Mode.RECEIVER, ProductStatus.COMPLETED);
        product(other, Type.SERVICE, Mode.GIVER, ProductStatus.ONGOING);
        em.flush();
        em.clear();

        assertThat(repository.findProductByMemberAndTypeAndModeAndStatusIn(owner, null, null, null))
                .extracting(Product::getId).containsExactlyInAnyOrder(ongoing.getId(), reservation.getId(), completed.getId());
        assertThat(repository.findProductByMemberAndTypeAndModeAndStatusIn(owner, null, null, List.of()))
                .extracting(Product::getId).containsExactlyInAnyOrder(ongoing.getId(), reservation.getId(), completed.getId());
        assertThat(repository.findProductByMemberAndTypeAndModeAndStatusIn(
                owner, Type.SERVICE, Mode.GIVER, List.of(ProductStatus.ONGOING, ProductStatus.RESERVATION)))
                .extracting(Product::getId).containsExactlyInAnyOrder(ongoing.getId(), reservation.getId());
    }

    @Nested
    @DisplayName("채팅방 상품 이미지 일괄 조회는")
    class Describe_findRoomProductsWithImagesByIds {

        @Test
        @DisplayName("상품 ID가 null이면 빈 목록을 반환한다")
        void it_returns_empty_when_product_ids_are_null() {
            assertThat(repository.findRoomProductsWithImagesByIds(null)).isEmpty();
        }

        @Test
        @DisplayName("상품 ID가 비어 있으면 빈 목록을 반환한다")
        void it_returns_empty_when_product_ids_are_empty() {
            assertThat(repository.findRoomProductsWithImagesByIds(List.of())).isEmpty();
        }
    }

    private Member member(String suffix) {
        return em.persist(Member.builder()
                .name("회원-" + suffix)
                .nickname("별명-" + suffix)
                .phoneNumber("010-0000-" + String.format("%04d", suffix.hashCode() & 0x7fff))
                .password("pw")
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Member memberWithDetail(String suffix, Place place) {
        Member member = member(suffix);
        em.persist(MemberDetail.builder()
                .member(member)
                .dong(em.persist(Dong.builder().name("동-" + suffix).build()))
                .place(place)
                .gwangsan(0)
                .light(0)
                .description("소개")
                .build());
        return member;
    }

    private Place place(String suffix) {
        Head head = em.persist(Head.builder().name("본부-" + suffix).build());
        return em.persist(Place.builder().name("지점-" + suffix).head(head).build());
    }

    private Product product(Member member, Type type, Mode mode, ProductStatus status) {
        return em.persist(Product.builder()
                .title("글-" + status + "-" + type + "-" + mode)
                .description("설명")
                .gwangsan(5000)
                .member(member)
                .type(type)
                .mode(mode)
                .status(status)
                .build());
    }

    private boolean isLoaded(Object association) {
        return em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(association);
    }
}
