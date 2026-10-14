package team.startup.gwangsan.application.suspend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.suspend.data.SuspendMemberCommand;
import team.startup.gwangsan.application.suspend.port.out.ChangeMemberStatusPort;
import team.startup.gwangsan.application.suspend.port.out.LoadAdminAlertPort;
import team.startup.gwangsan.application.suspend.port.out.RequestSuspensionAlertPort;
import team.startup.gwangsan.application.suspend.port.out.SaveSuspensionPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.suspend.model.Suspension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuspendMemberService 단위 테스트")
class SuspendMemberServiceTest {

    @Mock private ChangeMemberStatusPort changeMemberStatusPort;
    @Mock private SaveSuspensionPort saveSuspensionPort;
    @Mock private LoadAdminAlertPort loadAdminAlertPort;
    @Mock private RequestSuspensionAlertPort requestSuspensionAlertPort;

    @InjectMocks private SuspendMemberService service;

    @Nested
    @DisplayName("alertId가 있고 같은 ID의 관리자 알림이 없을 때")
    class Context_with_alert_id_and_no_admin_alert {

        @Test
        @DisplayName("it_정지 기간을 저장하고 회원을 정지한 뒤 신고 알림을 요청한다")
        void it_suspends_member_and_requests_alert() {
            when(saveSuspensionPort.save(any())).thenReturn(10L);
            when(loadAdminAlertPort.exists(5L)).thenReturn(false);

            service.execute(new SuspendMemberCommand(1L, 7, 5L));

            ArgumentCaptor<Suspension> captor = ArgumentCaptor.forClass(Suspension.class);
            InOrder order = inOrder(changeMemberStatusPort, saveSuspensionPort, requestSuspensionAlertPort);
            order.verify(changeMemberStatusPort).checkMemberExists(1L);
            order.verify(saveSuspensionPort).save(captor.capture());
            order.verify(changeMemberStatusPort).suspend(1L);
            order.verify(requestSuspensionAlertPort).requestReportAlert(5L, 1L, 10L);

            Suspension saved = captor.getValue();
            assertThat(saved.memberId()).isEqualTo(1L);
            assertThat(saved.suspendedDays()).isEqualTo(7);
            assertThat(saved.suspendedUntil()).isEqualTo(saved.suspendedAt().plusDays(7));
        }
    }

    @Nested
    @DisplayName("alertId가 null일 때")
    class Context_with_null_alert_id {

        @Test
        @DisplayName("it_정지만 하고 관리자 알림 조회·알림 요청을 하지 않는다")
        void it_suspends_member_without_alert() {
            service.execute(new SuspendMemberCommand(1L, 7, null));

            verify(changeMemberStatusPort).suspend(1L);
            verifyNoInteractions(loadAdminAlertPort, requestSuspensionAlertPort);
        }
    }

    @Nested
    @DisplayName("alertId가 있지만 같은 ID의 관리자 알림이 있을 때")
    class Context_with_existing_admin_alert {

        @Test
        @DisplayName("it_정지만 하고 알림을 요청하지 않는다")
        void it_suspends_member_without_alert() {
            when(loadAdminAlertPort.exists(5L)).thenReturn(true);

            service.execute(new SuspendMemberCommand(1L, 7, 5L));

            verify(changeMemberStatusPort).suspend(1L);
            verifyNoInteractions(requestSuspensionAlertPort);
        }
    }

    @Nested
    @DisplayName("회원이 없을 때")
    class Context_with_member_not_found {

        @Test
        @DisplayName("it_NotFoundMemberException 을 던지고 정지를 저장하지 않는다")
        void it_throws_not_found_member_exception() {
            doThrow(new NotFoundMemberException()).when(changeMemberStatusPort).checkMemberExists(99L);

            assertThatThrownBy(() -> service.execute(new SuspendMemberCommand(99L, 7, 5L)))
                    .isInstanceOf(NotFoundMemberException.class);

            verifyNoInteractions(saveSuspensionPort, requestSuspensionAlertPort);
        }
    }
}
