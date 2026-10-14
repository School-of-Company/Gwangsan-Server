package team.startup.gwangsan.application.admin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.admin.data.AdjustGwangsanCommand;
import team.startup.gwangsan.application.admin.data.UpdateMemberRoleCommand;
import team.startup.gwangsan.application.admin.data.UpdateMemberStatusCommand;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.admin.port.out.AdminPlacePort;
import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static team.startup.gwangsan.application.admin.service.AdminServiceFixture.*;

@DisplayName("관리자 회원 변경 유스케이스 (광산 조정·역할·상태)")
class AdminMemberCommandServicesTest {
    private final AdminMemberPort members = mock(AdminMemberPort.class);
    private final AdminPlacePort places = mock(AdminPlacePort.class);
    private final AdminScopeResolver resolver = new AdminScopeResolver(members, places);

    @Nested
    @DisplayName("AdjustGwangsanService 는")
    class Describe_adjust {
        private final AdjustGwangsanService service = new AdjustGwangsanService(resolver, members);

        @Test
        @DisplayName("it_같은 지점 회원의 광산을 음수 포함 그대로 조정한다")
        void it_adjusts_member_gwangsan() {
            givenAdmins(members, places);
            when(members.loadAdjustTargetPlaceId(5L)).thenReturn(OWN_PLACE);

            service.execute(new AdjustGwangsanCommand(PLACE_ADMIN, 5L, -3000));

            verify(members).adjustGwangsan(5L, -3000);
        }

        @Test
        @DisplayName("it_다른 지점 회원이면 조정하지 않는다")
        void it_rejects_other_place() {
            givenAdmins(members, places);
            when(members.loadAdjustTargetPlaceId(5L)).thenReturn(OTHER_PLACE);

            assertThatThrownBy(() -> service.execute(new AdjustGwangsanCommand(PLACE_ADMIN, 5L, 100)))
                    .isInstanceOf(AdminPlaceMismatchException.class);
            verify(members, never()).adjustGwangsan(anyLong(), any());
        }

        @Test
        @DisplayName("it_대상 회원 조회 실패는 회원 업무 예외를 그대로 전달한다")
        void it_propagates_not_found_member() {
            givenAdmins(members, places);
            when(members.loadAdjustTargetPlaceId(5L)).thenThrow(new NotFoundMemberException());

            assertThatThrownBy(() -> service.execute(new AdjustGwangsanCommand(PLACE_ADMIN, 5L, 100)))
                    .isInstanceOf(NotFoundMemberException.class);
        }
    }

    @Nested
    @DisplayName("UpdateMemberRoleService 는")
    class Describe_role {
        private final UpdateMemberRoleService service = new UpdateMemberRoleService(resolver, members);

        @Test
        @DisplayName("it_회원 존재와 지점을 확인한 뒤 역할과 지점을 변경한다")
        void it_updates_role_and_place() {
            givenAdmins(members, places);
            givenMemberPlace(members, 5L, 2);

            service.execute(new UpdateMemberRoleCommand(HEAD_ADMIN, 5L, ActorContext.Role.ROLE_PLACE_ADMIN, 2));

            var order = inOrder(members);
            order.verify(members).requireMember(5L);
            order.verify(members).loadPlace(5L);
            order.verify(members).changeRole(5L, ActorContext.Role.ROLE_PLACE_ADMIN, 2);
        }

        @Test
        @DisplayName("it_대상 회원이 없으면 변경하지 않는다")
        void it_stops_when_member_missing() {
            givenAdmins(members, places);
            doThrow(new NotFoundMemberException()).when(members).requireMember(5L);

            assertThatThrownBy(() -> service.execute(new UpdateMemberRoleCommand(HEAD_ADMIN, 5L, ActorContext.Role.ROLE_USER, null)))
                    .isInstanceOf(NotFoundMemberException.class);
            verify(members, never()).changeRole(anyLong(), any(), any());
        }

        @Test
        @DisplayName("it_다른 지점 회원이면 변경하지 않는다")
        void it_rejects_other_place() {
            givenAdmins(members, places);
            givenMemberPlace(members, 5L, OTHER_PLACE);

            assertThatThrownBy(() -> service.execute(new UpdateMemberRoleCommand(PLACE_ADMIN, 5L, ActorContext.Role.ROLE_USER, null)))
                    .isInstanceOf(AdminPlaceMismatchException.class);
            verify(members, never()).changeRole(anyLong(), any(), any());
        }
    }

    @Nested
    @DisplayName("UpdateMemberStatusService 는")
    class Describe_status {
        private final UpdateMemberStatusService service = new UpdateMemberStatusService(resolver, members);

        @Test
        @DisplayName("it_WITHDRAWN 이 아니면 상태만 바꾼다")
        void it_updates_member_status() {
            givenAdmins(members, places);
            givenMemberPlace(members, 5L, OWN_PLACE);

            service.execute(new UpdateMemberStatusCommand(PLACE_ADMIN, 5L, AccountStatus.SUSPENDED));

            verify(members).changeStatus(5L, AccountStatus.SUSPENDED);
            verify(members, never()).withdrawBanned(anyLong());
        }

        @Test
        @DisplayName("it_WITHDRAWN 이면 재가입 차단 탈퇴를 수행하고 상태 변경은 하지 않는다")
        void it_withdraws_banned_member() {
            givenAdmins(members, places);
            givenMemberPlace(members, 5L, OWN_PLACE);

            service.execute(new UpdateMemberStatusCommand(PLACE_ADMIN, 5L, AccountStatus.WITHDRAWN));

            verify(members).withdrawBanned(5L);
            verify(members, never()).changeStatus(anyLong(), any());
        }

        @Test
        @DisplayName("it_다른 지점 회원은 탈퇴시키지 않는다")
        void it_rejects_other_place() {
            givenAdmins(members, places);
            givenMemberPlace(members, 5L, OTHER_PLACE);

            assertThatThrownBy(() -> service.execute(new UpdateMemberStatusCommand(PLACE_ADMIN, 5L, AccountStatus.WITHDRAWN)))
                    .isInstanceOf(AdminPlaceMismatchException.class);
            verify(members, never()).withdrawBanned(anyLong());
        }
    }
}
