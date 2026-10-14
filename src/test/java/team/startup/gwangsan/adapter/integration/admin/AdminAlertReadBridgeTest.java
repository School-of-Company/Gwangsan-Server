package team.startup.gwangsan.adapter.integration.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertJpaEntity;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.adapter.persistence.post.Product;
import team.startup.gwangsan.adapter.persistence.report.ReportImageJpaEntity;
import team.startup.gwangsan.adapter.persistence.report.ReportImageJpaRepository;
import team.startup.gwangsan.adapter.persistence.report.ReportJpaEntity;
import team.startup.gwangsan.adapter.persistence.report.ReportJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelImageJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelImageJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaEntity;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaEntity;
import team.startup.gwangsan.adapter.web.post.dto.GetProductMemberResponse;
import team.startup.gwangsan.application.admin.data.AdminAlertsResult;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.model.ReportType;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAlertReadBridge 단위 테스트")
class AdminAlertReadBridgeTest {

    @InjectMocks private AdminAlertReadBridge bridge;
    @Mock private MemberDetailJpaRepository memberDetailRepository;
    @Mock private ReportJpaRepository reportRepository;
    @Mock private AdminAlertPersistenceAdapter adminAlertPersistenceAdapter;
    @Mock private MemberJpaRepository memberRepository;
    @Mock private ReportImageJpaRepository reportImageRepository;
    @Mock private ProductSnapshotQuery productSnapshotQuery;
    @Mock private ProductImageQuery productImageQuery;
    @Mock private TradeCancelJpaRepository tradeCancelRepository;
    @Mock private TradeCancelImageJpaRepository tradeCancelImageRepository;

    @Nested
    @DisplayName("findAlerts() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("알림이 없으면 세 유형 모두 빈 결과를 반환한다")
        void it_returns_all_empty_lists_without_alerts() {
            when(adminAlertPersistenceAdapter.findByPlacesAndType(List.of(7), AlertType.SIGN_UP)).thenReturn(List.of());
            when(memberDetailRepository.findPlaceNameMapByMemberIds(anySet())).thenReturn(Map.of());

            AdminAlertsResult response = bridge.findAlerts(List.of(7), AlertType.SIGN_UP);

            assertThat(response.reports()).isEmpty();
            assertThat(response.signUps()).isEmpty();
            assertThat(response.tradeCancels()).isEmpty();
        }

        @Test
        @DisplayName("실제 신고·가입·거래철회 데이터를 유형별 응답으로 변환한다")
        void it_maps_non_empty_alerts_for_all_types() {
            MemberJpaEntity requester = member(1L, "신고자", null, MemberRole.ROLE_USER);
            MemberJpaEntity reported = member(2L, "피신고자", null, MemberRole.ROLE_USER);
            MemberJpaEntity owner = member(3L, "작성자", null, MemberRole.ROLE_USER);
            MemberJpaEntity recommender = member(4L, "추천인", null, MemberRole.ROLE_USER);
            MemberJpaEntity joinedWithRecommender = member(20L, "가입자1", recommender, MemberRole.ROLE_USER);
            MemberJpaEntity joinedWithoutRecommender = member(21L, "가입자2", null, MemberRole.ROLE_USER);

            Product reservedProduct = product(300L, owner, ProductStatus.RESERVATION);
            Product completedProduct = product(301L, owner, ProductStatus.COMPLETED);
            ReportJpaEntity memberReport = report(10L, ReportType.SEXUAL, reported, null);
            ReportJpaEntity productReport = report(11L, ReportType.SPAM_AD, owner, reservedProduct);
            ReportJpaEntity missingTargetReport = report(12L, ReportType.ETC, null, null);
            TradeCancelJpaEntity withImages = cancel(40L, requester, reservedProduct, "이미지 포함 철회");
            TradeCancelJpaEntity withoutImages = cancel(41L, requester, completedProduct, "이미지 없는 철회");
            List<AdminAlertJpaEntity> alerts = List.of(
                    alert(100L, AlertType.REPORT, 10L, requester), alert(101L, AlertType.REPORT, 11L, requester),
                    alert(102L, AlertType.REPORT, 12L, requester), alert(103L, AlertType.SIGN_UP, 20L, joinedWithRecommender),
                    alert(104L, AlertType.SIGN_UP, 21L, joinedWithoutRecommender), alert(105L, AlertType.TRADE_CANCEL, 40L, requester),
                    alert(106L, AlertType.TRADE_CANCEL, 41L, requester)
            );
            ImageJpaEntity reportImage = image(500L, "report-url");
            ImageJpaEntity cancelImage = image(501L, "cancel-url");
            ImageJpaEntity productImage = image(502L, "product-url");

            when(adminAlertPersistenceAdapter.findByPlacesAndType(List.of(7), AlertType.REPORT)).thenReturn(alerts);
            when(memberDetailRepository.findPlaceNameMapByMemberIds(anySet())).thenReturn(Map.of(1L, "요청 지점", 20L, "가입 지점", 21L, "가입 지점"));
            when(reportRepository.findAllByIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(memberReport, productReport, missingTargetReport));
            when(reportImageRepository.findAllByReportIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(ReportImageJpaEntity.builder().report(memberReport).image(reportImage).build()));
            when(memberRepository.findAllByIdIn(List.of(20L, 21L))).thenReturn(List.of(joinedWithRecommender, joinedWithoutRecommender));
            when(tradeCancelRepository.findAllByIdIn(List.of(40L, 41L))).thenReturn(List.of(withImages, withoutImages));
            when(tradeCancelImageRepository.findByTradeCancelIn(List.of(40L, 41L))).thenReturn(List.of(TradeCancelImageJpaEntity.builder().tradeCancel(withImages).image(cancelImage).build()));
            when(productSnapshotQuery.findAll(anySet())).thenReturn(Map.of(
                    300L, new ProductSnapshot(300L, 3L, "상품 300", "설명", 100, Type.SERVICE, Mode.GIVER, ProductStatus.RESERVATION),
                    301L, new ProductSnapshot(301L, 3L, "상품 301", "설명", 100, Type.SERVICE, Mode.GIVER, ProductStatus.COMPLETED)));
            // post 공개 이미지 query 는 이미지 ID 를 준다(관리자 응답 imageId 계약과 같음).
            when(productImageQuery.findImages(anySet())).thenReturn(Map.of(300L, List.of(new ImageView(productImage.getId(), "product-url"))));
            when(memberRepository.findProductMemberResponsesByMemberIds(anySet())).thenReturn(List.of(new GetProductMemberResponse(3L, "작성자", "작성자 지점", 9)));

            AdminAlertsResult response = bridge.findAlerts(List.of(7), AlertType.REPORT);

            assertThat(response.reports()).hasSize(3);
            assertThat(response.reports().get(0)).satisfies(report -> {
                assertThat(report.productTarget()).isFalse();
                assertThat(report.reportedMemberId()).isEqualTo(2L);
                assertThat(report.images()).extracting("imageUrl").containsExactly("report-url");
            });
            assertThat(response.reports().get(1)).satisfies(report -> {
                assertThat(report.productTarget()).isTrue();
                assertThat(report.productId()).isEqualTo(300L);
                assertThat(report.productTitle()).isEqualTo("상품 300");
            });
            assertThat(response.reports().get(2).reportedMemberName()).isEqualTo("알 수 없음");
            assertThat(response.signUps()).extracting("recommenderNickname").containsExactly("추천인", null);
            assertThat(response.tradeCancels()).hasSize(2);
            assertThat(response.tradeCancels().get(0)).satisfies(cancel -> {
                assertThat(cancel.images()).extracting("imageUrl").containsExactly("cancel-url");
                assertThat(cancel.product().images()).extracting("imageUrl").containsExactly("product-url");
                assertThat(cancel.product().images()).extracting("imageId").containsExactly(502L);
                assertThat(cancel.product().member().nickname()).isEqualTo("작성자");
                assertThat(cancel.product().member().placeName()).isEqualTo("작성자 지점");
                assertThat(cancel.product().member().light()).isEqualTo(1);
                assertThat(cancel.product().reserved()).isTrue();
                assertThat(cancel.product().completed()).isFalse();
            });
            assertThat(response.tradeCancels().get(1)).satisfies(cancel -> {
                assertThat(cancel.images()).isEmpty();
                assertThat(cancel.product().images()).isEmpty();
                assertThat(cancel.product().completed()).isTrue();
                assertThat(cancel.product().reserved()).isFalse();
            });
        }

        @Test
        @DisplayName("신고 원본이 없으면 NotFoundReportException을 던진다")
        void it_throws_when_report_source_is_missing() {
            prepareSingleAlert(AlertType.REPORT, 10L);
            when(reportRepository.findAllByIdIn(List.of(10L))).thenReturn(List.of());
            assertThatThrownBy(() -> bridge.findAlerts(List.of(7), AlertType.REPORT)).isInstanceOf(NotFoundReportException.class);
        }

        @Test
        @DisplayName("가입 회원 원본이 없으면 NotFoundMemberException을 던진다")
        void it_throws_when_sign_up_source_is_missing() {
            prepareSingleAlert(AlertType.SIGN_UP, 20L);
            when(memberRepository.findAllByIdIn(List.of(20L))).thenReturn(List.of());
            assertThatThrownBy(() -> bridge.findAlerts(List.of(7), AlertType.SIGN_UP)).isInstanceOf(NotFoundMemberException.class);
        }

        @Test
        @DisplayName("거래 철회 원본이 없으면 NotFoundTradeCancelException을 던진다")
        void it_throws_when_trade_cancel_source_is_missing() {
            prepareSingleAlert(AlertType.TRADE_CANCEL, 40L);
            when(tradeCancelRepository.findAllByIdIn(List.of(40L))).thenReturn(List.of());
            assertThatThrownBy(() -> bridge.findAlerts(List.of(7), AlertType.TRADE_CANCEL)).isInstanceOf(NotFoundTradeCancelException.class);
        }
    }

    private void prepareSingleAlert(AlertType type, Long sourceId) {
        MemberJpaEntity requester = member(sourceId, "요청자", null, MemberRole.ROLE_USER);
        when(adminAlertPersistenceAdapter.findByPlacesAndType(List.of(7), type)).thenReturn(List.of(alert(1L, type, sourceId, requester)));
        when(memberDetailRepository.findPlaceNameMapByMemberIds(anySet())).thenReturn(Map.of(sourceId, "지점"));
    }

    private MemberJpaEntity member(Long id, String nickname, MemberJpaEntity recommender, MemberRole role) {
        MemberJpaEntity member = MemberJpaEntity.builder().name(nickname).nickname(nickname).phoneNumber(nickname).password("password")
                .recommender(recommender).role(role).status(MemberStatus.ACTIVE).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Product product(Long id, MemberJpaEntity owner, ProductStatus status) {
        Product product = Product.builder().title("상품 " + id).description("설명").gwangsan(100).member(owner)
                .type(Type.SERVICE).mode(Mode.GIVER).status(status).build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ReportJpaEntity report(Long id, ReportType type, MemberJpaEntity reported, Product product) {
        ReportJpaEntity report = ReportJpaEntity.builder().reportType(type).content("신고 내용").reporter(reported).reported(reported).product(product).build();
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    private TradeCancelJpaEntity cancel(Long id, MemberJpaEntity requester, Product product, String reason) {
        TradeCompleteJpaEntity complete = TradeCompleteJpaEntity.builder().product(product).buyer(requester).seller(product.getMember())
                .status(TradeStatus.PENDING).requestedBySeller(false).build();
        TradeCancelJpaEntity cancel = TradeCancelJpaEntity.builder().tradeComplete(complete).member(requester).reason(reason).status(TradeCancelStatus.PENDING).build();
        ReflectionTestUtils.setField(cancel, "id", id);
        return cancel;
    }

    private AdminAlertJpaEntity alert(Long id, AlertType type, Long sourceId, MemberJpaEntity requester) {
        AdminAlertJpaEntity alert = AdminAlertJpaEntity.builder().type(type).title(type + " 알림").sourceId(sourceId).requester(requester).build();
        ReflectionTestUtils.setField(alert, "id", id);
        return alert;
    }

    private ImageJpaEntity image(Long id, String url) {
        ImageJpaEntity image = ImageJpaEntity.builder().imageUrl(url).build();
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }
}
