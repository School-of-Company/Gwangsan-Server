package team.startup.gwangsan.adapter.persistence.trade;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.integration.trade.TradeImageAdapter;
import team.startup.gwangsan.adapter.persistence.image.ImagePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.application.image.service.ImageQueryService;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.adapter.persistence.review.ReviewJpaEntity;
import team.startup.gwangsan.adapter.persistence.review.ReviewPersistenceAdapter;
import team.startup.gwangsan.application.trade.data.TradeCancelImageView;
import team.startup.gwangsan.application.trade.data.TradeCancelView;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.trade.model.TradeCancel;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.model.TradeStatus;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 정리 라운드 A 의 trade/review provider 공개 Port 가 쓰는 저장소 경로를 H2 로 확인한다.
 * 기존 소비자(PR11 bridge)가 직접 부르던 Repository 메서드와 같은 쿼리를 쓰므로 결과·순서·쿼리 수가 같아야 한다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({QueryDslConfig.class, TradeCancelPersistenceAdapter.class, TradeMemberReferencePersistenceAdapter.class,
        TradeCompletePersistenceAdapter.class,
        ReviewPersistenceAdapter.class, TradeImageAdapter.class, ImageQueryService.class, ImagePersistenceAdapter.class})
@DisplayName("trade/review provider 공개 Port 저장소 경로")
class TradeProviderPortPersistenceTest {

    @Autowired private TestEntityManager em;
    @Autowired private EntityManagerFactory emf;
    @Autowired private TradeCancelPersistenceAdapter tradeCancelAdapter;
    @Autowired private TradeMemberReferencePersistenceAdapter memberReferenceAdapter;
    @Autowired private ReviewPersistenceAdapter reviewAdapter;
    @Autowired private TradeImageAdapter tradeImageAdapter;
    @Autowired private TradeCompletePersistenceAdapter tradeCompleteAdapter;
    @Autowired private TradeCompleteJpaRepository tradeCompleteJpaRepository;
    @Autowired private TradeCancelJpaRepository tradeCancelJpaRepository;
    @Autowired private team.startup.gwangsan.adapter.persistence.review.ReviewJpaRepository reviewJpaRepository;

    private MemberJpaEntity buyer;
    private MemberJpaEntity seller;
    private MemberJpaEntity dummy;
    private Product product;
    private Product otherProduct;
    private TradeCancelJpaEntity withImages;
    private TradeCancelJpaEntity withoutImages;
    private ImageJpaEntity first;
    private ImageJpaEntity second;

    @BeforeEach
    void setUp() {
        buyer = member("buyer", "010-1");
        seller = member("seller", "010-2");
        dummy = member("dummy", "010-9");
        product = em.persist(Product.builder().title("상품").description("설명").gwangsan(100).member(seller)
                .type(Type.SERVICE).mode(Mode.GIVER).status(ProductStatus.COMPLETED).build());
        TradeCompleteJpaEntity trade = em.persist(TradeCompleteJpaEntity.builder().product(product).buyer(buyer).seller(seller)
                .status(TradeStatus.COMPLETED).requestedBySeller(true).build());
        otherProduct = em.persist(Product.builder().title("다른").description("설명").gwangsan(100).member(seller)
                .type(Type.SERVICE).mode(Mode.GIVER).status(ProductStatus.COMPLETED).build());
        TradeCompleteJpaEntity otherTrade = em.persist(TradeCompleteJpaEntity.builder().product(otherProduct).buyer(buyer).seller(seller)
                .status(TradeStatus.COMPLETED).requestedBySeller(true).build());
        withImages = em.persist(TradeCancelJpaEntity.builder().tradeComplete(trade).member(buyer).reason("사유1")
                .status(TradeCancelStatus.PENDING).build());
        withoutImages = em.persist(TradeCancelJpaEntity.builder().tradeComplete(otherTrade).member(seller).reason("사유2")
                .status(TradeCancelStatus.APPROVED).build());
        first = em.persist(ImageJpaEntity.builder().imageUrl("https://img/1").build());
        second = em.persist(ImageJpaEntity.builder().imageUrl("https://img/2").build());
        em.persist(TradeCancelImageJpaEntity.builder().tradeCancel(withImages).image(first).build());
        em.persist(TradeCancelImageJpaEntity.builder().tradeCancel(withImages).image(second).build());
        em.flush();
        em.clear();
        statistics().clear();
    }

    @Nested
    @DisplayName("관리자 조회 query 는")
    class Describe_admin_queries {

        @Test
        @DisplayName("철회 요청 목록을 1 쿼리로 상품 ID·요청자·사유·상태와 함께 돌려주고 없는 ID 는 뺀다")
        void it_finds_views_in_one_query() {
            List<TradeCancelView> views = tradeCancelAdapter.findViews(List.of(withImages.getId(), withoutImages.getId(), 9999L));

            assertThat(views).containsExactlyInAnyOrder(
                    new TradeCancelView(withImages.getId(), product.getId(), buyer.getId(), "사유1", TradeCancelStatus.PENDING),
                    new TradeCancelView(withoutImages.getId(), otherProduct.getId(), seller.getId(), "사유2", TradeCancelStatus.APPROVED));
            assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("철회별 이미지를 1 쿼리로 저장 순서대로 묶고 이미지 없는 요청은 key 가 없다")
        void it_groups_images_in_one_query() {
            Map<Long, List<TradeCancelImageView>> images = tradeCancelAdapter.findImageViews(List.of(withImages.getId(), withoutImages.getId()));

            assertThat(images).containsOnlyKeys(withImages.getId());
            assertThat(images.get(withImages.getId())).containsExactly(
                    new TradeCancelImageView(first.getId(), "https://img/1"),
                    new TradeCancelImageView(second.getId(), "https://img/2"));
            assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("요청자 ID 를 1 쿼리로 찾고 없으면 비어 있다")
        void it_finds_requester() {
            assertThat(tradeCancelAdapter.findRequesterId(withImages.getId())).contains(buyer.getId());
            assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
            assertThat(tradeCancelAdapter.findRequesterId(9999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("알림 원천 요약 query 는 기존 저장 모델 탐색과 같은 값·같은 쿼리 수다")
    class Describe_summaries {

        @Test
        @DisplayName("거래 요약: 거래 1회 + 상품·구매자 지연 로딩(기존 tradeCompleteRepository.findById 경로와 같은 수)")
        void it_summarizes_trade_complete() {
            long tradeId = em.find(TradeCancelJpaEntity.class, withImages.getId()).getTradeComplete().getId();
            em.clear();
            statistics().clear();
            TradeCompleteJpaEntity legacy = tradeCompleteJpaRepository.findById(tradeId).orElseThrow();
            String legacyTitle = legacy.getProduct().getTitle();
            String legacyNickname = legacy.getBuyer().getNickname();
            long legacyCount = statistics().getPrepareStatementCount();
            em.clear();
            statistics().clear();

            var summary = tradeCompleteAdapter.findSummary(tradeId).orElseThrow();

            assertThat(statistics().getPrepareStatementCount()).isEqualTo(legacyCount);
            assertThat(summary.productId()).isEqualTo(product.getId());
            assertThat(summary.productTitle()).isEqualTo(legacyTitle).isEqualTo("상품");
            assertThat(summary.buyerId()).isEqualTo(buyer.getId());
            assertThat(summary.buyerNickname()).isEqualTo(legacyNickname).isEqualTo("buyer");
            assertThat(summary.sellerId()).isEqualTo(seller.getId());
            assertThat(tradeCompleteAdapter.findSummary(99999L)).isEmpty();
        }

        @Test
        @DisplayName("철회 당사자: fetch join 1회로 요청자·구매자·판매자")
        void it_finds_cancel_parties() {
            var parties = tradeCancelAdapter.findParties(withoutImages.getId()).orElseThrow();

            assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
            assertThat(parties.requesterId()).isEqualTo(seller.getId());
            assertThat(parties.buyerId()).isEqualTo(buyer.getId());
            assertThat(parties.sellerId()).isEqualTo(seller.getId());
            assertThat(tradeCancelAdapter.findParties(99999L)).isEmpty();
        }

        @Test
        @DisplayName("후기 작성자: 후기 1회 + 작성자 지연 로딩(기존 reviewRepository.findById 경로와 같은 수)")
        void it_finds_review_author() {
            ReviewJpaEntity review = em.persist(ReviewJpaEntity.builder().product(em.find(Product.class, product.getId()))
                    .reviewer(em.find(MemberJpaEntity.class, buyer.getId())).reviewed(em.find(MemberJpaEntity.class, seller.getId()))
                    .content("후기").light(10).build());
            em.flush();
            em.clear();
            statistics().clear();
            String legacyNickname = reviewJpaRepository.findById(review.getId()).orElseThrow().getReviewer().getNickname();
            long legacyCount = statistics().getPrepareStatementCount();
            em.clear();
            statistics().clear();

            var author = reviewAdapter.findAuthor(review.getId()).orElseThrow();

            assertThat(statistics().getPrepareStatementCount()).isEqualTo(legacyCount);
            assertThat(author.reviewerId()).isEqualTo(buyer.getId());
            assertThat(author.reviewerNickname()).isEqualTo(legacyNickname).isEqualTo("buyer");
            assertThat(reviewAdapter.findAuthor(99999L)).isEmpty();
        }
    }

    @Test
    @DisplayName("철회 첨부: image 공개 query 로 존재하는 이미지만 골라(1 쿼리) 연결 행을 저장한다")
    void it_attaches_only_existing_images() {
        List<Long> existing = tradeImageAdapter.findExistingImageIds(List.of(first.getId(), 99999L, second.getId()));
        assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
        assertThat(existing).containsExactlyInAnyOrder(first.getId(), second.getId());

        tradeCancelAdapter.attachImages(withoutImages.getId(), existing);
        em.flush();
        em.clear();

        assertThat(tradeCancelAdapter.findImageViews(List.of(withoutImages.getId())).get(withoutImages.getId()))
                .extracting(TradeCancelImageView::imageId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    @DisplayName("기각 경로: 연관 없이 읽은 철회를 REJECTED 로 저장한다")
    void it_rejects_through_basic_load() {
        TradeCancel cancel = tradeCancelAdapter.findBasicById(withImages.getId()).orElseThrow();
        cancel.reject();
        tradeCancelAdapter.update(cancel);
        em.flush();
        em.clear();

        assertThat(em.find(TradeCancelJpaEntity.class, withImages.getId()).getStatus()).isEqualTo(TradeCancelStatus.REJECTED);
        assertThat(cancel.requesterId()).isEqualTo(buyer.getId());
    }

    @Test
    @DisplayName("회원 정리: 거래 구매자·판매자, 철회 요청자, 후기 작성자·대상을 dummy 로 재지정한다")
    void it_reassigns_member_references() {
        ReviewJpaEntity review = em.persist(ReviewJpaEntity.builder().product(em.find(Product.class, product.getId()))
                .reviewer(em.find(MemberJpaEntity.class, buyer.getId())).reviewed(em.find(MemberJpaEntity.class, seller.getId()))
                .content("후기").light(10).build());
        em.flush();
        em.clear();

        memberReferenceAdapter.reassignBuyer(buyer.getId(), dummy.getId());
        memberReferenceAdapter.reassignSeller(seller.getId(), dummy.getId());
        memberReferenceAdapter.reassignCancelRequester(buyer.getId(), dummy.getId());
        reviewAdapter.reassignReviewer(buyer.getId(), dummy.getId());
        reviewAdapter.reassignReviewed(seller.getId(), dummy.getId());

        TradeCancelJpaEntity cancel = em.find(TradeCancelJpaEntity.class, withImages.getId());
        assertThat(cancel.getTradeComplete().getBuyer().getId()).isEqualTo(dummy.getId());
        assertThat(cancel.getTradeComplete().getSeller().getId()).isEqualTo(dummy.getId());
        assertThat(cancel.getMember().getId()).isEqualTo(dummy.getId());
        assertThat(em.find(TradeCancelJpaEntity.class, withoutImages.getId()).getMember().getId()).isEqualTo(seller.getId());
        ReviewJpaEntity stored = em.find(ReviewJpaEntity.class, review.getId());
        assertThat(stored.getReviewer().getId()).isEqualTo(dummy.getId());
        assertThat(stored.getReviewed().getId()).isEqualTo(dummy.getId());
    }

    private Statistics statistics() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    private MemberJpaEntity member(String nickname, String phone) {
        return em.persist(MemberJpaEntity.builder().name(nickname).nickname(nickname).password("pw").phoneNumber(phone)
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build());
    }
}
