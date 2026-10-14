package team.startup.gwangsan.application.admin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.admin.data.AdminAlertSearch;
import team.startup.gwangsan.application.admin.data.AdminAlertsResult;
import team.startup.gwangsan.application.admin.data.AdminPlace;
import team.startup.gwangsan.application.admin.data.CreateAdminAlertCommand;
import team.startup.gwangsan.application.admin.port.out.*;
import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundPendingMemberException;
import team.startup.gwangsan.domain.admin.model.AdminAlert;
import team.startup.gwangsan.domain.admin.policy.AdminAlertTitle;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static team.startup.gwangsan.application.admin.service.AdminServiceFixture.*;

@DisplayName("관리자 알림 생성·조회·공개 능력")
class AdminAlertCreationServicesTest {
    private final AdminAlertPort alerts = mock(AdminAlertPort.class);
    private final AdminMemberPort members = mock(AdminMemberPort.class);
    private final AdminPlacePort places = mock(AdminPlacePort.class);
    private final AdminReportPort reports = mock(AdminReportPort.class);
    private final AdminTradeCancelPort tradeCancels = mock(AdminTradeCancelPort.class);
    private final AdminAlertReadPort reads = mock(AdminAlertReadPort.class);

    @Nested
    @DisplayName("CreateAdminAlertService 는")
    class Describe_create {
        private final CreateAdminAlertService service = new CreateAdminAlertService(alerts, members, reports, tradeCancels);

        @Test
        @DisplayName("it_신고 알림은 신고 종류 제목과 요청 회원으로 저장한다")
        void it_saves_report_alert() {
            when(reports.categoryOf(10L)).thenReturn(AdminAlertTitle.ReportCategory.SPAM_AD);

            service.execute(new CreateAdminAlertCommand(AlertType.REPORT, 10L, 3L));

            verify(members).requireMember(3L);
            verify(alerts).save(AlertType.REPORT, "스팸/광고 신고", 10L, 3L);
        }

        @Test
        @DisplayName("it_신고 요청 회원이 없으면 저장하지 않는다")
        void it_rejects_missing_reporter() {
            when(reports.categoryOf(10L)).thenReturn(AdminAlertTitle.ReportCategory.ETC);
            doThrow(new NotFoundMemberException()).when(members).requireMember(3L);

            assertThatThrownBy(() -> service.execute(new CreateAdminAlertCommand(AlertType.REPORT, 10L, 3L)))
                    .isInstanceOf(NotFoundMemberException.class);
            verify(alerts, never()).save(any(), any(), any(), any());
        }

        @Test
        @DisplayName("it_가입 알림은 승인 대기 회원을 요청자로 저장한다")
        void it_saves_sign_up_alert() {
            when(members.isPendingMember(20L)).thenReturn(true);

            service.execute(new CreateAdminAlertCommand(AlertType.SIGN_UP, 20L, 99L));

            verify(alerts).save(AlertType.SIGN_UP, "새로운 회원가입 요청", 20L, 20L);
        }

        @Test
        @DisplayName("it_승인 대기 회원이 아니면 NotFoundPendingMemberException 이다")
        void it_throws_not_found_pending_member() {
            when(members.isPendingMember(20L)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(new CreateAdminAlertCommand(AlertType.SIGN_UP, 20L, 99L)))
                    .isInstanceOf(NotFoundPendingMemberException.class);
        }

        @Test
        @DisplayName("it_철회 알림은 철회 요청 회원으로 저장한다")
        void it_saves_trade_cancel_alert() {
            when(tradeCancels.requesterIdOf(40L)).thenReturn(7L);

            service.execute(new CreateAdminAlertCommand(AlertType.TRADE_CANCEL, 40L, 99L));

            verify(alerts).save(AlertType.TRADE_CANCEL, "거래 철회 요청", 40L, 7L);
        }
    }

    @Nested
    @DisplayName("FindAdminAlertsService 는")
    class Describe_find {
        private final FindAdminAlertsService service = new FindAdminAlertsService(members, places, reads);
        private final AdminAlertsResult empty = new AdminAlertsResult(List.of(), List.of(), List.of());

        @Test
        @DisplayName("it_본부 관리자는 지정 지점만 조회한다")
        void it_uses_explicit_place_for_head_admin() {
            when(places.requirePlace(7)).thenReturn(7);
            when(reads.findAlerts(List.of(7), AlertType.SIGN_UP)).thenReturn(empty);

            assertThat(service.query(new AdminAlertSearch(HEAD_ADMIN, 7, AlertType.SIGN_UP))).isEqualTo(empty);
        }

        @Test
        @DisplayName("it_본부 관리자는 지점 지정이 없으면 본부 산하 지점 전체를 조회한다")
        void it_uses_head_places() {
            when(members.loadViewerPlace(HEAD_ADMIN.memberId())).thenReturn(new AdminPlace(1, 10));
            when(places.placeIdsInHead(10)).thenReturn(List.of(1, 2));
            when(reads.findAlerts(List.of(1, 2), null)).thenReturn(empty);

            assertThat(service.query(new AdminAlertSearch(HEAD_ADMIN, null, null))).isEqualTo(empty);
        }

        @Test
        @DisplayName("it_지점 관리자는 place_id 를 무시하고 자기 지점만 조회한다")
        void it_ignores_place_filter_for_place_admin() {
            when(members.loadViewerPlace(PLACE_ADMIN.memberId())).thenReturn(new AdminPlace(1, 10));
            when(reads.findAlerts(List.of(1), null)).thenReturn(empty);

            service.query(new AdminAlertSearch(PLACE_ADMIN, 7, null));

            verify(places, never()).requirePlace(any());
            verify(reads).findAlerts(List.of(1), null);
        }

        @Test
        @DisplayName("it_없는 지정 지점은 PlaceNotFoundException 이다")
        void it_throws_when_explicit_place_missing() {
            when(places.requirePlace(7)).thenThrow(new PlaceNotFoundException());

            assertThatThrownBy(() -> service.query(new AdminAlertSearch(HEAD_ADMIN, 7, null)))
                    .isInstanceOf(PlaceNotFoundException.class);
            verifyNoInteractions(reads);
        }
    }

    @Nested
    @DisplayName("AdminAlertSupportService 는")
    class Describe_support {
        private final AdminAlertSupportService service = new AdminAlertSupportService(alerts);

        @Test
        @DisplayName("it_남은 알림이 있을 때만 지운다")
        void it_removes_only_existing_alert() {
            when(alerts.findByTypeAndSourceId(AlertType.TRADE_CANCEL, 40L))
                    .thenReturn(Optional.of(new AdminAlert(100L, AlertType.TRADE_CANCEL, "t", 40L, 5L, LocalDateTime.now())));
            when(alerts.findByTypeAndSourceId(AlertType.TRADE_CANCEL, 41L)).thenReturn(Optional.empty());

            service.removeBySource(AlertType.TRADE_CANCEL, 40L);
            service.removeBySource(AlertType.TRADE_CANCEL, 41L);

            verify(alerts, times(1)).delete(anyLong());
            verify(alerts).delete(100L);
        }

        @Test
        @DisplayName("it_존재 확인과 탈퇴 재지정을 위임한다")
        void it_delegates_exists_and_reassign() {
            when(alerts.existsById(5L)).thenReturn(true);

            assertThat(service.exists(5L)).isTrue();
            service.reassign(1L, 2L);

            verify(alerts).reassignMember(1L, 2L);
        }
    }
}
