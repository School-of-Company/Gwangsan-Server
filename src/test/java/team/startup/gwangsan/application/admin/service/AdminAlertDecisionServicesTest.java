package team.startup.gwangsan.application.admin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import team.startup.gwangsan.application.admin.data.AdminAlertCommand;
import team.startup.gwangsan.application.admin.data.MemberAlertKind;
import team.startup.gwangsan.application.admin.data.SignUpTarget;
import team.startup.gwangsan.application.admin.port.out.*;
import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.model.AdminAlert;
import team.startup.gwangsan.domain.trade.exception.CannotPendingTradeCancelException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static team.startup.gwangsan.application.admin.service.AdminServiceFixture.*;

@DisplayName("관리자 알림 처리 유스케이스 (승인·기각·가입 승인·삭제)")
class AdminAlertDecisionServicesTest {
    private static final Long ALERT_ID = 100L;
    private static final Long REQUESTER_ID = 5L;

    private final AdminAlertPort alerts = mock(AdminAlertPort.class);
    private final AdminMemberPort members = mock(AdminMemberPort.class);
    private final AdminPlacePort places = mock(AdminPlacePort.class);
    private final AdminTradeCancelPort tradeCancels = mock(AdminTradeCancelPort.class);
    private final AdminReportPort reports = mock(AdminReportPort.class);
    private final AdminNotificationPort notifications = mock(AdminNotificationPort.class);
    private final AdminScopeResolver resolver = new AdminScopeResolver(members, places);

    private AdminAlert alert(AlertType type, Long sourceId) {
        return new AdminAlert(ALERT_ID, type, "제목", sourceId, REQUESTER_ID, LocalDateTime.now());
    }

    @Nested
    @DisplayName("AdminApproveTradeCancelService 는")
    class Describe_approve {
        private final AdminApproveTradeCancelService service =
                new AdminApproveTradeCancelService(alerts, resolver, members, tradeCancels);

        private void givenTradeCancel(Integer buyerPlace, Integer sellerPlace) {
            givenAdmins(members, places);
            when(alerts.findWithRequester(ALERT_ID)).thenReturn(Optional.of(alert(AlertType.TRADE_CANCEL, 40L)));
            // 거래 업무 승인 command 가 잠금·철회 로드 뒤 구매자(7)·판매자(8)로 관리자 검사를 부르는 것을 흉내 낸다.
            doAnswer(invocation -> {
                invocation.<AdminTradeCancelPort.ParticipantCheck>getArgument(1).check(7L, 8L);
                tradeCancels.requesterIdOf(-1L); // 검사 통과 뒤에만 기록되는 적용 표시
                return null;
            }).when(tradeCancels).approve(eq(40L), any());
            givenMemberPlace(members, 7L, buyerPlace);
            givenMemberPlace(members, 8L, sellerPlace);
        }

        @Test
        @DisplayName("it_알림·관리자 범위를 확인한 뒤 거래 승인 command 에 위임하고, 참여자 지점 검사는 그 안(잠금·철회 로드 뒤)에서 한다")
        void it_delegates_to_trade_approval_with_participant_check() {
            givenTradeCancel(OWN_PLACE, OWN_PLACE);

            service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID));

            InOrder order = inOrder(alerts, tradeCancels, members);
            order.verify(alerts).findWithRequester(ALERT_ID);
            order.verify(members).loadPlace(PLACE_ADMIN.memberId());
            order.verify(tradeCancels).approve(eq(40L), any());
            order.verify(members).loadPlace(7L);
            order.verify(members).loadPlace(8L);
            order.verify(tradeCancels).requesterIdOf(-1L);
            // 상품 ID 스칼라 조회·잠금·철회 로드 순서는 거래 업무 ApproveTradeCancelService 가 소유한다(PR07 테스트·PR12 SQL 관찰).
            verify(tradeCancels, never()).productIdOf(anyLong());
            verify(tradeCancels, never()).lockProduct(anyLong());
        }

        @Test
        @DisplayName("it_구매자나 판매자 중 하나라도 다른 지점이면 반영하지 않는다")
        void it_validates_both_sides() {
            givenTradeCancel(OWN_PLACE, OTHER_PLACE);

            assertThatThrownBy(() -> service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID)))
                    .isInstanceOf(AdminPlaceMismatchException.class);
            verify(tradeCancels, never()).requesterIdOf(-1L);
        }

        @Test
        @DisplayName("it_알림이 없으면 잠금도 잡지 않는다")
        void it_throws_not_found_admin_alert() {
            when(alerts.findWithRequester(ALERT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID)))
                    .isInstanceOf(NotFoundAdminAlertException.class);
            verifyNoInteractions(tradeCancels);
        }
    }

    @Nested
    @DisplayName("RejectAdminAlertService 는")
    class Describe_reject {
        private final RejectAdminAlertService service =
                new RejectAdminAlertService(resolver, alerts, members, tradeCancels, reports, notifications);

        private void givenAlert(AlertType type, Long sourceId, Integer requesterPlace) {
            givenAdmins(members, places);
            when(alerts.findById(ALERT_ID)).thenReturn(Optional.of(alert(type, sourceId)));
            givenMemberPlace(members, REQUESTER_ID, requesterPlace);
        }

        @Test
        @DisplayName("it_가입 기각은 알림과 신청자 상세를 지운다")
        void it_rejects_sign_up() {
            givenAlert(AlertType.SIGN_UP, REQUESTER_ID, OWN_PLACE);

            service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID));

            verify(alerts).delete(ALERT_ID);
            verify(members).rejectSignUp(REQUESTER_ID);
            verifyNoInteractions(tradeCancels, notifications);
        }

        @Test
        @DisplayName("it_신고 기각은 신고자에게 REPORT_REJECT 알림을 보낸다")
        void it_notifies_reporter() {
            givenAlert(AlertType.REPORT, 10L, OWN_PLACE);
            when(reports.reporterIdOf(10L)).thenReturn(3L);

            service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID));

            verify(notifications).notifyMember(10L, 3L, MemberAlertKind.REPORT_REJECT);
            verifyNoInteractions(tradeCancels);
        }

        @Test
        @DisplayName("it_철회 기각은 상품 잠금 후 REJECTED 로 바꾸고 요청자에게 알린다")
        void it_rejects_trade_cancel_after_lock() {
            givenAlert(AlertType.TRADE_CANCEL, 40L, OWN_PLACE);
            when(tradeCancels.productIdOf(40L)).thenReturn(300L);
            when(tradeCancels.rejectPending(40L)).thenReturn(REQUESTER_ID);

            service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID));

            InOrder order = inOrder(tradeCancels, alerts, notifications);
            order.verify(tradeCancels).productIdOf(40L);
            order.verify(tradeCancels).lockProduct(300L);
            order.verify(alerts).delete(ALERT_ID);
            order.verify(tradeCancels).rejectPending(40L);
            order.verify(notifications).notifyMember(40L, REQUESTER_ID, MemberAlertKind.TRADE_CANCEL_REJECT);
        }

        @Test
        @DisplayName("it_이미 처리된 철회는 거래 업무 예외를 전달하고 알리지 않는다")
        void it_propagates_already_handled() {
            givenAlert(AlertType.TRADE_CANCEL, 40L, OWN_PLACE);
            when(tradeCancels.productIdOf(40L)).thenReturn(300L);
            when(tradeCancels.rejectPending(40L)).thenThrow(new CannotPendingTradeCancelException());

            assertThatThrownBy(() -> service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID)))
                    .isInstanceOf(CannotPendingTradeCancelException.class);
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("it_다른 지점 요청자의 알림은 지우지 않는다")
        void it_rejects_other_place() {
            givenAlert(AlertType.SIGN_UP, REQUESTER_ID, OTHER_PLACE);

            assertThatThrownBy(() -> service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID)))
                    .isInstanceOf(AdminPlaceMismatchException.class);
            verify(alerts, never()).delete(anyLong());
        }

        @Test
        @DisplayName("it_알림이 없으면 NotFoundAdminAlertException 이다")
        void it_throws_not_found_admin_alert() {
            givenAdmins(members, places);
            when(alerts.findById(ALERT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID)))
                    .isInstanceOf(NotFoundAdminAlertException.class);
        }
    }

    @Nested
    @DisplayName("VerifySignUpService 는")
    class Describe_verify {
        private final VerifySignUpService service = new VerifySignUpService(resolver, alerts, members, notifications);

        private void givenSignUp(Integer place, Optional<Integer> withdrawn) {
            givenAdmins(members, places);
            when(alerts.findByIdAndType(ALERT_ID, AlertType.SIGN_UP)).thenReturn(Optional.of(alert(AlertType.SIGN_UP, 20L)));
            when(members.loadSignUpTarget(20L)).thenReturn(new SignUpTarget(20L, "010", place, 4L));
            when(members.rewardRecommender(4L, 20L, 5000)).thenReturn(44L);
            when(members.takeWithdrawnGwangsan("010")).thenReturn(withdrawn);
        }

        @Test
        @DisplayName("it_탈퇴 이력이 있으면 그 광산을 복원한다")
        void it_restores_withdrawn_gwangsan() {
            givenSignUp(OWN_PLACE, Optional.of(3200));

            service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID));

            verify(members).creditSignUpReward(20L, 3200);
        }

        @Test
        @DisplayName("it_신규 회원은 10000 을 받고 추천인은 5000 과 RECOMMENDER 알림을 받는다")
        void it_rewards_new_member_and_recommender() {
            givenSignUp(OWN_PLACE, Optional.empty());

            service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID));

            InOrder order = inOrder(members, alerts, notifications);
            order.verify(members).rewardRecommender(4L, 20L, 5000);
            order.verify(members).activate(20L);
            order.verify(members).creditSignUpReward(20L, 10000);
            order.verify(alerts).delete(ALERT_ID);
            order.verify(notifications).notifyMember(20L, 44L, MemberAlertKind.RECOMMENDER);
        }

        @Test
        @DisplayName("it_다른 지점 가입자는 승인하지 않는다(추천인 보상은 같은 트랜잭션에서 rollback)")
        void it_rejects_other_place() {
            givenSignUp(OTHER_PLACE, Optional.empty());

            assertThatThrownBy(() -> service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID)))
                    .isInstanceOf(AdminPlaceMismatchException.class);
            verify(members, never()).activate(anyLong());
            verifyNoInteractions(notifications);
        }

        @Test
        @DisplayName("it_가입 알림이 없으면 NotFoundAdminAlertException 이다")
        void it_throws_not_found_admin_alert() {
            givenAdmins(members, places);
            when(alerts.findByIdAndType(ALERT_ID, AlertType.SIGN_UP)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID)))
                    .isInstanceOf(NotFoundAdminAlertException.class);
        }
    }

    @Nested
    @DisplayName("DeleteAdminAlertService 는")
    class Describe_delete {
        private final DeleteAdminAlertService service = new DeleteAdminAlertService(resolver, alerts, members);

        @Test
        @DisplayName("it_같은 지점 요청자의 알림을 지운다")
        void it_deletes_admin_alert() {
            givenAdmins(members, places);
            when(alerts.findById(ALERT_ID)).thenReturn(Optional.of(alert(AlertType.REPORT, 10L)));
            givenMemberPlace(members, REQUESTER_ID, OWN_PLACE);

            service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID));

            verify(alerts).delete(ALERT_ID);
        }

        @Test
        @DisplayName("it_알림이 없으면 NotFoundAdminAlertException 이다")
        void it_throws_not_found_admin_alert() {
            givenAdmins(members, places);
            when(alerts.findById(ALERT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new AdminAlertCommand(PLACE_ADMIN, ALERT_ID)))
                    .isInstanceOf(NotFoundAdminAlertException.class);
            verify(alerts, never()).delete(anyLong());
        }
    }
}
