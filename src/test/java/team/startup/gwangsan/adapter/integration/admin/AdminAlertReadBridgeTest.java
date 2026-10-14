package team.startup.gwangsan.adapter.integration.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.admin.AdminAlertPersistenceAdapter.AlertRow;
import team.startup.gwangsan.application.admin.data.AdminAlertsResult;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.in.ProductImageQuery;
import team.startup.gwangsan.application.post.port.in.ProductSnapshotQuery;
import team.startup.gwangsan.application.report.data.ReportDetail;
import team.startup.gwangsan.application.report.port.in.ReportQuery;
import team.startup.gwangsan.application.trade.data.TradeCancelImageView;
import team.startup.gwangsan.application.trade.data.TradeCancelView;
import team.startup.gwangsan.application.trade.port.in.TradeCancelQuery;
import team.startup.gwangsan.domain.admin.model.AdminAlert;
import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.report.exception.NotFoundReportException;
import team.startup.gwangsan.domain.report.model.ReportType;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.model.TradeCancelStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@DisplayName("AdminAlertReadBridge 단위 테스트")
class AdminAlertReadBridgeTest {
    private final AdminAlertPersistenceAdapter alerts = mock(AdminAlertPersistenceAdapter.class);
    private final MemberSummaryQuery members = mock(MemberSummaryQuery.class);
    private final ReportQuery reports = mock(ReportQuery.class);
    private final TradeCancelQuery tradeCancels = mock(TradeCancelQuery.class);
    private final ProductSnapshotQuery products = mock(ProductSnapshotQuery.class);
    private final ProductImageQuery productImages = mock(ProductImageQuery.class);
    private final AdminAlertReadBridge bridge = new AdminAlertReadBridge(alerts, members, reports, tradeCancels,
            products, productImages);
    private final LocalDateTime at = LocalDateTime.of(2026, 10, 14, 9, 0);

    private AlertRow row(Long id, AlertType type, Long sourceId, Long requesterId, String nickname) {
        return new AlertRow(new AdminAlert(id, type, type + " 알림", sourceId, requesterId, at), nickname);
    }

    private MemberSummary member(Long id, String nickname, String recommenderNickname, String placeName, Integer light) {
        return new MemberSummary(id, nickname, "010" + id, null, recommenderNickname,
                placeName == null ? null : 1, placeName == null ? null : 10, placeName, light);
    }

    @Nested
    @DisplayName("findAlerts() 메서드는")
    class Describe_findAlerts {

        @Test
        @DisplayName("it_알림이 없으면 세 유형 모두 빈 결과를 반환한다")
        void it_returns_all_empty_lists_without_alerts() {
            when(alerts.findByPlacesAndType(List.of(7), AlertType.SIGN_UP)).thenReturn(List.of());

            AdminAlertsResult result = bridge.findAlerts(List.of(7), AlertType.SIGN_UP);

            assertThat(result.reports()).isEmpty();
            assertThat(result.signUps()).isEmpty();
            assertThat(result.tradeCancels()).isEmpty();
            verifyNoInteractions(reports, tradeCancels);
        }

        @Test
        @DisplayName("it_신고·가입·거래철회 데이터를 유형별 결과로 변환한다")
        void it_maps_non_empty_alerts_for_all_types() {
            when(alerts.findByPlacesAndType(List.of(7), null)).thenReturn(List.of(
                    row(100L, AlertType.REPORT, 10L, 1L, "신고자"), row(101L, AlertType.REPORT, 11L, 1L, "신고자"),
                    row(102L, AlertType.REPORT, 12L, 1L, "신고자"), row(103L, AlertType.SIGN_UP, 20L, 20L, "가입자1"),
                    row(104L, AlertType.SIGN_UP, 21L, 21L, "가입자2"), row(105L, AlertType.TRADE_CANCEL, 40L, 1L, "신고자"),
                    row(106L, AlertType.TRADE_CANCEL, 41L, 1L, "신고자")));
            when(members.findAllByIds(Set.of(1L, 20L, 21L))).thenReturn(List.of(
                    member(1L, "신고자", null, "요청 지점", 30), member(20L, "가입자1", "추천인", "가입 지점", 10),
                    member(21L, "가입자2", null, "가입 지점", 10)));
            when(reports.findDetails(List.of(10L, 11L, 12L))).thenReturn(Map.of(
                    10L, new ReportDetail(10L, ReportType.SEXUAL, "내용", 2L, "피신고자", null, null,
                            List.of(new ReportDetail.ReportImageRef(500L, "report-url"))),
                    11L, new ReportDetail(11L, ReportType.SPAM_AD, "내용", 3L, "작성자", 300L, "상품 300", List.of()),
                    12L, new ReportDetail(12L, ReportType.ETC, "내용", null, null, null, null, List.of())));
            when(members.findAllByIds(List.of(20L, 21L))).thenReturn(List.of(
                    member(20L, "가입자1", "추천인", "가입 지점", 10), member(21L, "가입자2", null, "가입 지점", 10)));
            when(tradeCancels.findAll(List.of(40L, 41L))).thenReturn(List.of(
                    new TradeCancelView(40L, 300L, 1L, "이미지 포함 철회", TradeCancelStatus.PENDING),
                    new TradeCancelView(41L, 301L, 1L, "이미지 없는 철회", TradeCancelStatus.PENDING)));
            when(tradeCancels.findImages(List.of(40L, 41L))).thenReturn(Map.of(40L, List.of(new TradeCancelImageView(501L, "cancel-url"))));
            when(products.findAll(Set.of(300L, 301L))).thenReturn(Map.of(
                    300L, new ProductSnapshot(300L, 3L, "상품 300", "설명", 100, Type.SERVICE, Mode.GIVER, ProductStatus.RESERVATION),
                    301L, new ProductSnapshot(301L, 3L, "상품 301", "설명", 100, Type.SERVICE, Mode.GIVER, ProductStatus.COMPLETED)));
            when(productImages.findImages(Set.of(300L, 301L))).thenReturn(Map.of(300L, List.of(new ImageView(502L, "product-url"))));
            when(members.findAllByIds(Set.of(3L))).thenReturn(List.of(member(3L, "작성자", null, "작성자 지점", 9)));

            AdminAlertsResult result = bridge.findAlerts(List.of(7), null);

            assertThat(result.reports()).hasSize(3);
            assertThat(result.reports().get(0)).satisfies(report -> {
                assertThat(report.productTarget()).isFalse();
                assertThat(report.reportedMemberId()).isEqualTo(2L);
                assertThat(report.nickname()).isEqualTo("신고자");
                assertThat(report.placeName()).isEqualTo("요청 지점");
                assertThat(report.images()).extracting("imageId").containsExactly(500L);
            });
            assertThat(result.reports().get(1)).satisfies(report -> {
                assertThat(report.productTarget()).isTrue();
                assertThat(report.productId()).isEqualTo(300L);
                assertThat(report.productTitle()).isEqualTo("상품 300");
            });
            assertThat(result.reports().get(2).reportedMemberName()).isEqualTo("알 수 없음");
            assertThat(result.reports().get(2).reportedMemberId()).isNull();
            assertThat(result.signUps()).extracting("recommenderNickname").containsExactly("추천인", null);
            assertThat(result.tradeCancels()).hasSize(2);
            assertThat(result.tradeCancels().get(0)).satisfies(cancel -> {
                assertThat(cancel.reason()).isEqualTo("이미지 포함 철회");
                assertThat(cancel.images()).extracting("imageId").containsExactly(501L);
                assertThat(cancel.product().images()).extracting("imageId").containsExactly(502L);
                assertThat(cancel.product().member().nickname()).isEqualTo("작성자");
                assertThat(cancel.product().member().placeName()).isEqualTo("작성자 지점");
                assertThat(cancel.product().member().light()).isEqualTo(1);
                assertThat(cancel.product().reserved()).isTrue();
                assertThat(cancel.product().completed()).isFalse();
            });
            assertThat(result.tradeCancels().get(1)).satisfies(cancel -> {
                assertThat(cancel.images()).isEmpty();
                assertThat(cancel.product().images()).isEmpty();
                assertThat(cancel.product().completed()).isTrue();
            });
        }

        @Test
        @DisplayName("it_신고 원본이 없으면 NotFoundReportException 이다")
        void it_throws_when_report_source_is_missing() {
            when(alerts.findByPlacesAndType(List.of(7), AlertType.REPORT)).thenReturn(List.of(row(1L, AlertType.REPORT, 10L, 10L, "요청자")));
            when(reports.findDetails(List.of(10L))).thenReturn(Map.of());

            assertThatThrownBy(() -> bridge.findAlerts(List.of(7), AlertType.REPORT)).isInstanceOf(NotFoundReportException.class);
        }

        @Test
        @DisplayName("it_가입 회원 원본이 없으면 NotFoundMemberException 이다")
        void it_throws_when_sign_up_source_is_missing() {
            when(alerts.findByPlacesAndType(List.of(7), AlertType.SIGN_UP)).thenReturn(List.of(row(1L, AlertType.SIGN_UP, 20L, 20L, "요청자")));
            when(members.findAllByIds(anyCollection())).thenReturn(List.of());

            assertThatThrownBy(() -> bridge.findAlerts(List.of(7), AlertType.SIGN_UP)).isInstanceOf(NotFoundMemberException.class);
        }

        @Test
        @DisplayName("it_거래 철회 원본이 없으면 NotFoundTradeCancelException 이다")
        void it_throws_when_trade_cancel_source_is_missing() {
            when(alerts.findByPlacesAndType(List.of(7), AlertType.TRADE_CANCEL)).thenReturn(List.of(row(1L, AlertType.TRADE_CANCEL, 40L, 40L, "요청자")));
            when(tradeCancels.findAll(List.of(40L))).thenReturn(List.of());
            when(products.findAll(any())).thenReturn(Map.of());
            when(productImages.findImages(anyCollection())).thenReturn(Map.of());

            assertThatThrownBy(() -> bridge.findAlerts(List.of(7), AlertType.TRADE_CANCEL)).isInstanceOf(NotFoundTradeCancelException.class);
        }
    }
}
