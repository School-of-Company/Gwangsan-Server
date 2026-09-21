package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.presentation.dto.response.GetAdminAlertResponse;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductMemberResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.ReportImage;
import team.startup.gwangsan.domain.report.entity.constant.ReportTargetType;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeCancelImage;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.entity.constant.TradeCancelStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelImageRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAlertByAlertTypeAndPlaceServiceImpl 단위 테스트")
class FindAlertByAlertTypeAndPlaceServiceImplTest {

    @InjectMocks private FindAlertByAlertTypeAndPlaceServiceImpl service;
    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private AdminAlertRepository adminAlertRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ReportImageRepository reportImageRepository;
    @Mock private PlaceRepository placeRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private TradeCancelRepository tradeCancelRepository;
    @Mock private TradeCancelImageRepository tradeCancelImageRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("지점 관리자는 알림이 없으면 세 유형 모두 빈 응답을 반환한다")
        void it_returns_all_empty_lists_for_place_admin_without_alerts() {
            Member admin = member(90L, "지점 관리자", null, MemberRole.ROLE_PLACE_ADMIN);
            Place place = Place.builder().name("관리자 지점").build();
            when(memberUtil.getCurrentMember()).thenReturn(admin);
            when(memberDetailRepository.findPlaceByMemberId(90L)).thenReturn(place);
            when(adminAlertRepository.findAdminAlertByPlacesAndAlertType(List.of(place), AlertType.SIGN_UP)).thenReturn(List.of());
            when(memberDetailRepository.findPlaceNameMapByMemberIds(anySet())).thenReturn(Map.of());

            GetAdminAlertResponse response = service.execute(null, AlertType.SIGN_UP);

            assertThat(response.reports()).isEmpty();
            assertThat(response.signUps()).isEmpty();
            assertThat(response.tradeCancels()).isEmpty();
        }

        @Test
        @DisplayName("실제 신고·가입·거래철회 데이터를 유형별 응답으로 변환한다")
        void it_maps_non_empty_alerts_for_all_types() {
            Member admin = member(90L, "본부 관리자", null, MemberRole.ROLE_PLACE_ADMIN);
            Member requester = member(1L, "신고자", null, MemberRole.ROLE_USER);
            Member reported = member(2L, "피신고자", null, MemberRole.ROLE_USER);
            Member owner = member(3L, "작성자", null, MemberRole.ROLE_USER);
            Member recommender = member(4L, "추천인", null, MemberRole.ROLE_USER);
            Member joinedWithRecommender = member(20L, "가입자1", recommender, MemberRole.ROLE_USER);
            Member joinedWithoutRecommender = member(21L, "가입자2", null, MemberRole.ROLE_USER);
            Place place = Place.builder().name("광산 지점").build();

            Product reservedProduct = product(300L, owner, ProductStatus.RESERVATION);
            Product completedProduct = product(301L, owner, ProductStatus.COMPLETED);
            Report memberReport = report(10L, ReportType.SEXUAL, reported, null);
            Report productReport = report(11L, ReportType.SPAM_AD, owner, reservedProduct);
            Report missingTargetReport = report(12L, ReportType.ETC, null, null);
            TradeCancel withImages = cancel(40L, requester, reservedProduct, "이미지 포함 철회");
            TradeCancel withoutImages = cancel(41L, requester, completedProduct, "이미지 없는 철회");
            List<AdminAlert> alerts = List.of(
                    alert(100L, AlertType.REPORT, 10L, requester), alert(101L, AlertType.REPORT, 11L, requester),
                    alert(102L, AlertType.REPORT, 12L, requester), alert(103L, AlertType.SIGN_UP, 20L, joinedWithRecommender),
                    alert(104L, AlertType.SIGN_UP, 21L, joinedWithoutRecommender), alert(105L, AlertType.TRADE_CANCEL, 40L, requester),
                    alert(106L, AlertType.TRADE_CANCEL, 41L, requester)
            );
            Image reportImage = image(500L, "report-url");
            Image cancelImage = image(501L, "cancel-url");
            Image productImage = image(502L, "product-url");

            when(memberUtil.getCurrentMember()).thenReturn(admin);
            when(memberDetailRepository.findPlaceByMemberId(90L)).thenReturn(place);
            when(adminAlertRepository.findAdminAlertByPlacesAndAlertType(List.of(place), AlertType.REPORT)).thenReturn(alerts);
            when(memberDetailRepository.findPlaceNameMapByMemberIds(anySet())).thenReturn(Map.of(1L, "요청 지점", 20L, "가입 지점", 21L, "가입 지점"));
            when(reportRepository.findAllByIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(memberReport, productReport, missingTargetReport));
            when(reportImageRepository.findAllByReportIdIn(List.of(10L, 11L, 12L))).thenReturn(List.of(ReportImage.builder().report(memberReport).image(reportImage).build()));
            when(memberRepository.findAllByIdIn(List.of(20L, 21L))).thenReturn(List.of(joinedWithRecommender, joinedWithoutRecommender));
            when(tradeCancelRepository.findAllByIdIn(List.of(40L, 41L))).thenReturn(List.of(withImages, withoutImages));
            when(tradeCancelImageRepository.findByTradeCancelIn(List.of(40L, 41L))).thenReturn(List.of(TradeCancelImage.builder().tradeCancel(withImages).image(cancelImage).build()));
            when(productImageRepository.findAllByProductIdIn(anySet())).thenReturn(List.of(ProductImage.builder().product(reservedProduct).image(productImage).build()));
            when(memberRepository.findProductMemberResponsesByMemberIds(anySet())).thenReturn(List.of(new GetProductMemberResponse(3L, "작성자", "작성자 지점", 9)));

            GetAdminAlertResponse response = service.execute(null, AlertType.REPORT);

            assertThat(response.reports()).hasSize(3);
            assertThat(response.reports().get(0)).satisfies(report -> {
                assertThat(report.targetType()).isEqualTo(ReportTargetType.MEMBER);
                assertThat(report.reportedMemberId()).isEqualTo(2L);
                assertThat(report.report().images()).extracting("imageUrl").containsExactly("report-url");
            });
            assertThat(response.reports().get(1)).satisfies(report -> {
                assertThat(report.targetType()).isEqualTo(ReportTargetType.PRODUCT);
                assertThat(report.productId()).isEqualTo(300L);
                assertThat(report.productTitle()).isEqualTo("상품 300");
            });
            assertThat(response.reports().get(2).reportedMemberName()).isEqualTo("알 수 없음");
            assertThat(response.signUps()).extracting("recommenderNickname").containsExactly("추천인", null);
            assertThat(response.tradeCancels()).hasSize(2);
            assertThat(response.tradeCancels().get(0)).satisfies(cancel -> {
                assertThat(cancel.images()).extracting("imageUrl").containsExactly("cancel-url");
                assertThat(cancel.product().images()).extracting("imageUrl").containsExactly("product-url");
                assertThat(cancel.product().member().light()).isEqualTo(1);
                assertThat(cancel.product().isReserved()).isTrue();
                assertThat(cancel.product().isCompleted()).isFalse();
            });
            assertThat(response.tradeCancels().get(1)).satisfies(cancel -> {
                assertThat(cancel.images()).isEmpty();
                assertThat(cancel.product().images()).isEmpty();
                assertThat(cancel.product().isCompleted()).isTrue();
                assertThat(cancel.product().isReserved()).isFalse();
            });
        }

        @Test
        @DisplayName("신고 원본이 없으면 NotFoundReportException을 던진다")
        void it_throws_when_report_source_is_missing() {
            prepareSingleAlert(AlertType.REPORT, 10L);
            when(reportRepository.findAllByIdIn(List.of(10L))).thenReturn(List.of());
            assertThatThrownBy(() -> service.execute(null, AlertType.REPORT)).isInstanceOf(NotFoundReportException.class);
        }

        @Test
        @DisplayName("가입 회원 원본이 없으면 NotFoundMemberException을 던진다")
        void it_throws_when_sign_up_source_is_missing() {
            prepareSingleAlert(AlertType.SIGN_UP, 20L);
            when(memberRepository.findAllByIdIn(List.of(20L))).thenReturn(List.of());
            assertThatThrownBy(() -> service.execute(null, AlertType.SIGN_UP)).isInstanceOf(NotFoundMemberException.class);
        }

        @Test
        @DisplayName("거래 철회 원본이 없으면 NotFoundTradeCancelException을 던진다")
        void it_throws_when_trade_cancel_source_is_missing() {
            prepareSingleAlert(AlertType.TRADE_CANCEL, 40L);
            when(tradeCancelRepository.findAllByIdIn(List.of(40L))).thenReturn(List.of());
            assertThatThrownBy(() -> service.execute(null, AlertType.TRADE_CANCEL)).isInstanceOf(NotFoundTradeCancelException.class);
        }

        @Test
        @DisplayName("본부 관리자는 지정 지점의 알림을 조회한다")
        void it_uses_explicit_place_for_head_admin() {
            Member admin = member(90L, "본부", null, MemberRole.ROLE_HEAD_ADMIN);
            Place place = Place.builder().name("지정 지점").build();
            ReflectionTestUtils.setField(place, "id", 7);
            when(memberUtil.getCurrentMember()).thenReturn(admin);
            when(placeRepository.findById(7)).thenReturn(Optional.of(place));
            when(adminAlertRepository.findAdminAlertByPlacesAndAlertType(List.of(place), AlertType.SIGN_UP)).thenReturn(List.of());
            when(memberDetailRepository.findPlaceNameMapByMemberIds(anySet())).thenReturn(Map.of());
            assertThat(service.execute(7, AlertType.SIGN_UP).signUps()).isEmpty();
        }

        @Test
        @DisplayName("본부 관리자는 지점 지정 없이 자신의 본부 산하 지점을 모두 조회한다")
        void it_uses_all_head_places_when_place_is_not_specified() {
            Member admin = member(90L, "본부", null, MemberRole.ROLE_HEAD_ADMIN);
            Place adminPlace = Place.builder().name("관리자 지점").build();
            when(memberUtil.getCurrentMember()).thenReturn(admin);
            when(memberDetailRepository.findPlaceByMemberId(90L)).thenReturn(adminPlace);
            when(placeRepository.findByHead(null)).thenReturn(List.of());
            when(adminAlertRepository.findAdminAlertByPlacesAndAlertType(List.of(), AlertType.SIGN_UP)).thenReturn(List.of());
            when(memberDetailRepository.findPlaceNameMapByMemberIds(anySet())).thenReturn(Map.of());

            GetAdminAlertResponse response = service.execute(null, AlertType.SIGN_UP);

            assertThat(response.reports()).isEmpty();
            assertThat(response.signUps()).isEmpty();
            assertThat(response.tradeCancels()).isEmpty();
        }

        @Test
        @DisplayName("본부 관리자가 없는 지정 지점을 조회하면 PlaceNotFoundException을 던진다")
        void it_throws_when_explicit_place_is_missing() {
            when(memberUtil.getCurrentMember()).thenReturn(member(90L, "본부", null, MemberRole.ROLE_HEAD_ADMIN));
            when(placeRepository.findById(7)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.execute(7, AlertType.SIGN_UP)).isInstanceOf(PlaceNotFoundException.class);
        }
    }

    private void prepareSingleAlert(AlertType type, Long sourceId) {
        Member admin = member(90L, "관리자", null, MemberRole.ROLE_PLACE_ADMIN);
        Member requester = member(sourceId, "요청자", null, MemberRole.ROLE_USER);
        Place place = Place.builder().name("지점").build();
        when(memberUtil.getCurrentMember()).thenReturn(admin);
        when(memberDetailRepository.findPlaceByMemberId(90L)).thenReturn(place);
        when(adminAlertRepository.findAdminAlertByPlacesAndAlertType(List.of(place), type)).thenReturn(List.of(alert(1L, type, sourceId, requester)));
        when(memberDetailRepository.findPlaceNameMapByMemberIds(anySet())).thenReturn(Map.of(sourceId, "지점"));
    }

    private Member member(Long id, String nickname, Member recommender, MemberRole role) {
        Member member = Member.builder().name(nickname).nickname(nickname).phoneNumber(nickname).password("password")
                .recommender(recommender).role(role).status(MemberStatus.ACTIVE).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Product product(Long id, Member owner, ProductStatus status) {
        Product product = Product.builder().title("상품 " + id).description("설명").gwangsan(100).member(owner)
                .type(Type.SERVICE).mode(Mode.GIVER).status(status).build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Report report(Long id, ReportType type, Member reported, Product product) {
        Report report = Report.builder().reportType(type).content("신고 내용").reporter(reported).reported(reported).product(product).build();
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    private TradeCancel cancel(Long id, Member requester, Product product, String reason) {
        TradeComplete complete = TradeComplete.builder().product(product).buyer(requester).seller(product.getMember())
                .status(TradeStatus.PENDING).requestedBySeller(false).build();
        TradeCancel cancel = TradeCancel.builder().tradeComplete(complete).member(requester).reason(reason).status(TradeCancelStatus.PENDING).build();
        ReflectionTestUtils.setField(cancel, "id", id);
        return cancel;
    }

    private AdminAlert alert(Long id, AlertType type, Long sourceId, Member requester) {
        AdminAlert alert = AdminAlert.builder().type(type).title(type + " 알림").sourceId(sourceId).requester(requester).build();
        ReflectionTestUtils.setField(alert, "id", id);
        return alert;
    }

    private Image image(Long id, String url) {
        Image image = Image.builder().imageUrl(url).build();
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }
}
