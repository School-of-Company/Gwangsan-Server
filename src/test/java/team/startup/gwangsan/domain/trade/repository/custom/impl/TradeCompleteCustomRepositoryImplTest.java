package team.startup.gwangsan.domain.trade.repository.custom.impl;

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
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("TradeCompleteCustomRepositoryImpl H2 통합 테스트")
class TradeCompleteCustomRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JPAQueryFactory queryFactory;

    private TradeCompleteCustomRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new TradeCompleteCustomRepositoryImpl(queryFactory);
    }

    @Nested
    @DisplayName("지점별 완료 거래 수 조회는")
    class Describe_countByPlaceId {

        @Test
        @DisplayName("시작 시각은 포함하고 종료 시각과 다른 상태 및 지점의 거래는 제외한다")
        void it_counts_only_completed_trades_in_the_requested_half_open_range() {
            Head head = createHead("본부-범위");
            Place targetPlace = createPlace(head, "지점-범위");
            Place otherPlace = createPlace(head, "지점-다른범위");
            Member buyer = createMember("buyer-range", "010-1000-0001");
            Member targetSeller = createMemberAtPlace(targetPlace, "seller-range", "010-1000-0002");
            Member otherSeller = createMemberAtPlace(otherPlace, "seller-other", "010-1000-0003");
            LocalDateTime start = LocalDateTime.of(2024, 1, 10, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 11, 0, 0);

            createTrade(buyer, targetSeller, TradeStatus.COMPLETED, start);
            createTrade(buyer, targetSeller, TradeStatus.COMPLETED, end.minusSeconds(1));
            createTrade(buyer, targetSeller, TradeStatus.COMPLETED, start.minusSeconds(1));
            createTrade(buyer, targetSeller, TradeStatus.COMPLETED, end);
            createTrade(buyer, targetSeller, TradeStatus.PENDING, start.plusHours(1));
            createTrade(buyer, otherSeller, TradeStatus.COMPLETED, start.plusHours(1));

            em.clear();

            assertThat(repository.countByPlaceIdBetween(targetPlace.getId(), start, end)).isEqualTo(2L);
        }

        @Test
        @DisplayName("period는 기준일 자정부터 오늘 다음 날 자정 전까지의 완료 거래를 센다")
        void it_uses_calendar_day_boundaries_for_period_counts() {
            Head head = createHead("본부-기간");
            Place place = createPlace(head, "지점-기간");
            Member buyer = createMember("buyer-period", "010-2000-0001");
            Member seller = createMemberAtPlace(place, "seller-period", "010-2000-0002");
            LocalDateTime now = LocalDateTime.of(2024, 1, 10, 12, 0);

            createTrade(buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 1, 8, 0, 0));
            createTrade(buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 1, 10, 23, 59, 59));
            createTrade(buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 1, 7, 23, 59, 59));
            createTrade(buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 1, 11, 0, 0));

            em.clear();

            assertThat(repository.countByPlaceId(3, now, place.getId())).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("본부별 완료 거래 수 조회는")
    class Describe_countByHeadId {

        @Test
        @DisplayName("거래가 없는 지점도 0으로 포함해 지점 ID 순서의 결과를 반환한다")
        void it_includes_places_without_completed_trades() {
            Head head = createHead("본부-집계");
            Place countedPlace = createPlace(head, "지점-집계-1");
            Place emptyPlace = createPlace(head, "지점-집계-2");
            Member buyer = createMember("buyer-head", "010-3000-0001");
            Member seller = createMemberAtPlace(countedPlace, "seller-head", "010-3000-0002");
            LocalDateTime start = LocalDateTime.of(2024, 2, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 2, 2, 0, 0);

            createTrade(buyer, seller, TradeStatus.COMPLETED, start.plusHours(1));

            em.clear();

            Map<Integer, Long> result = repository.countByHeadIdBetween(head.getId(), start, end);

            assertThat(result).containsExactly(
                    Map.entry(countedPlace.getId(), 1L),
                    Map.entry(emptyPlace.getId(), 0L)
            );
        }

        @Test
        @DisplayName("period는 본부 산하 지점의 기준일 포함 기간 완료 거래만 센다")
        void it_uses_calendar_day_boundaries_for_head_period_counts() {
            Head head = createHead("본부-기간");
            Place place = createPlace(head, "지점-본부기간");
            Member buyer = createMember("buyer-head-period", "010-4000-0001");
            Member seller = createMemberAtPlace(place, "seller-head-period", "010-4000-0002");
            LocalDateTime now = LocalDateTime.of(2024, 1, 10, 12, 0);

            createTrade(buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 1, 8, 0, 0));
            createTrade(buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 1, 10, 23, 59, 59));
            createTrade(buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 1, 7, 23, 59, 59));
            createTrade(buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 1, 11, 0, 0));

            em.clear();

            assertThat(repository.countByHeadId(3, now, head.getId()))
                    .containsExactly(Map.entry(place.getId(), 2L));
        }
    }

    @Nested
    @DisplayName("상품과 상태로 완료 거래 조회는")
    class Describe_findByProductIdAndStatus {

        @Test
        @DisplayName("일치하는 거래와 상품을 반환한다")
        void it_returns_the_trade_matching_product_and_status() {
            Member buyer = createMember("buyer-product-match", "010-5000-0001");
            Member seller = createMember("seller-product-match", "010-5000-0002");
            Product product = createProduct(seller, "상품-상품상태일치");
            TradeComplete matching = createTrade(product, buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 3, 1, 10, 0));

            em.clear();

            assertThat(repository.findByProductIdAndStatus(product.getId(), TradeStatus.COMPLETED))
                    .hasValueSatisfying(found -> {
                        assertThat(found.getId()).isEqualTo(matching.getId());
                        assertThat(isLoaded(found.getProduct())).isTrue();
                    });
        }

        @Test
        @DisplayName("없는 상품이면 빈 결과를 반환한다")
        void it_returns_empty_when_product_is_missing() {
            em.clear();

            assertThat(repository.findByProductIdAndStatus(999_999L, TradeStatus.COMPLETED)).isEmpty();
        }

        @Test
        @DisplayName("상품은 같아도 상태가 다르면 빈 결과를 반환한다")
        void it_returns_empty_when_status_does_not_match() {
            Member buyer = createMember("buyer-product-status", "010-5000-0003");
            Member seller = createMember("seller-product-status", "010-5000-0004");
            Product product = createProduct(seller, "상품-상품상태불일치");
            createTrade(product, buyer, seller, TradeStatus.PENDING, null);

            em.clear();

            assertThat(repository.findByProductIdAndStatus(product.getId(), TradeStatus.COMPLETED)).isEmpty();
        }
    }

    @Nested
    @DisplayName("ID로 상품과 회원을 포함한 완료 거래 조회는")
    class Describe_findByIdWithProductAndMember {

        @Test
        @DisplayName("상품, 구매자, 판매자를 즉시 조회한다")
        void it_fetches_product_buyer_and_seller() {
            Member buyer = createMember("buyer-fetch", "010-6000-0001");
            Member seller = createMember("seller-fetch", "010-6000-0002");
            TradeComplete trade = createTrade(buyer, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 3, 2, 10, 0));

            em.clear();

            assertThat(repository.findByIdWithProductAndMember(trade.getId()))
                    .hasValueSatisfying(found -> {
                        assertThat(isLoaded(found.getProduct())).isTrue();
                        assertThat(isLoaded(found.getBuyer())).isTrue();
                        assertThat(isLoaded(found.getSeller())).isTrue();
                    });
        }

        @Test
        @DisplayName("없는 거래 ID면 빈 결과를 반환한다")
        void it_returns_empty_when_trade_is_missing() {
            em.clear();

            assertThat(repository.findByIdWithProductAndMember(999_999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("회원과 상태로 완료 거래 목록 조회는")
    class Describe_findAllByMemberAndStatus {

        @Test
        @DisplayName("구매자 또는 판매자 거래만 완료 시각 내림차순, null 마지막, ID 내림차순으로 반환한다")
        void it_includes_buyer_or_seller_trades_and_orders_completed_at_with_nulls_last_and_id_ties() {
            Member member = createMember("member-history", "010-7000-0001");
            Member buyer = createMember("buyer-history", "010-7000-0002");
            Member seller = createMember("seller-history", "010-7000-0003");
            Member outsider = createMember("outsider-history", "010-7000-0004");
            TradeComplete newest = createTrade(member, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 3, 3, 12, 0));
            TradeComplete sellerTrade = createTrade(buyer, member, TradeStatus.COMPLETED, LocalDateTime.of(2024, 3, 3, 11, 0));
            TradeComplete firstTie = createTrade(member, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 3, 3, 10, 0));
            TradeComplete secondTie = createTrade(member, seller, TradeStatus.COMPLETED, LocalDateTime.of(2024, 3, 3, 10, 0));
            TradeComplete withoutCompletion = createTrade(member, seller, TradeStatus.COMPLETED, null);
            createTrade(member, seller, TradeStatus.PENDING, LocalDateTime.of(2024, 3, 3, 13, 0));
            createTrade(buyer, outsider, TradeStatus.COMPLETED, LocalDateTime.of(2024, 3, 3, 13, 0));

            em.clear();

            List<TradeComplete> found = repository.findAllByMemberAndStatus(member, TradeStatus.COMPLETED);

            assertThat(found)
                    .extracting(TradeComplete::getId)
                    .containsExactly(
                            newest.getId(),
                            sellerTrade.getId(),
                            secondTie.getId(),
                            firstTie.getId(),
                            withoutCompletion.getId()
                    );
            assertThat(found)
                    .allSatisfy(trade -> {
                        assertThat(isLoaded(trade.getProduct())).isTrue();
                        assertThat(isLoaded(trade.getBuyer())).isTrue();
                        assertThat(isLoaded(trade.getSeller())).isTrue();
                    });
        }
    }

    private Head createHead(String name) {
        return em.persist(Head.builder().name(name).build());
    }

    private Place createPlace(Head head, String name) {
        return em.persist(Place.builder().name(name).head(head).build());
    }

    private Member createMember(String nickname, String phone) {
        return em.persist(Member.builder()
                .name(nickname)
                .nickname(nickname)
                .password("pw")
                .phoneNumber(phone)
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private Member createMemberAtPlace(Place place, String nickname, String phone) {
        Member member = createMember(nickname, phone);
        em.persist(MemberDetail.builder()
                .member(member)
                .dong(em.persist(Dong.builder().name("동-" + nickname).build()))
                .place(place)
                .gwangsan(0)
                .light(0)
                .description("소개")
                .build());
        return member;
    }

    private TradeComplete createTrade(Member buyer, Member seller, TradeStatus status, LocalDateTime completedAt) {
        return createTrade(createProduct(seller, "상품-" + completedAt + "-" + status), buyer, seller, status, completedAt);
    }

    private Product createProduct(Member seller, String title) {
        return em.persist(Product.builder()
                .title(title)
                .description("설명")
                .gwangsan(5000)
                .member(seller)
                .type(Type.SERVICE)
                .mode(Mode.GIVER)
                .status(ProductStatus.ONGOING)
                .build());
    }

    private TradeComplete createTrade(Product product, Member buyer, Member seller, TradeStatus status, LocalDateTime completedAt) {
        TradeComplete trade = em.persist(TradeComplete.builder()
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .status(status)
                .requestedBySeller(true)
                .build());
        em.flush();
        if (completedAt != null) {
            em.getEntityManager().createNativeQuery("UPDATE tbl_trade_complete SET completed_at = ? WHERE trade_id = ?")
                    .setParameter(1, Timestamp.valueOf(completedAt))
                    .setParameter(2, trade.getId())
                    .executeUpdate();
        }
        return trade;
    }

    private boolean isLoaded(Object association) {
        return em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(association);
    }
}
