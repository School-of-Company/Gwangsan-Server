package team.startup.gwangsan.application.admin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import team.startup.gwangsan.application.admin.data.AdminCredential;
import team.startup.gwangsan.application.admin.data.AdminSignInCommand;
import team.startup.gwangsan.application.admin.data.AdminSignInResult;
import team.startup.gwangsan.application.admin.data.IssuedToken;
import team.startup.gwangsan.application.admin.port.out.AdminAuthPort;
import team.startup.gwangsan.domain.admin.exception.AdminForbiddenException;
import team.startup.gwangsan.domain.admin.exception.AdminPendingApprovalException;
import team.startup.gwangsan.domain.admin.exception.AdminUnauthorizedException;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SignInAdminService 단위 테스트")
class SignInAdminServiceTest {
    private final AdminAuthPort auth = mock(AdminAuthPort.class);
    private final SignInAdminService service = new SignInAdminService(auth);
    private final AdminSignInCommand command = new AdminSignInCommand("관리자", "password");

    private void given(ActorContext.Role role, AccountStatus status, boolean passwordMatches) {
        when(auth.loadCredential("관리자")).thenReturn(new AdminCredential("010", "encoded", role, status));
        when(auth.passwordMatches("password", "encoded")).thenReturn(passwordMatches);
    }

    @Test
    @DisplayName("it_활성 관리자에게 토큰과 역할을 반환한다")
    void it_returns_tokens() {
        given(ActorContext.Role.ROLE_HEAD_ADMIN, AccountStatus.ACTIVE, true);
        LocalDateTime at = LocalDateTime.of(2026, 10, 14, 0, 0);
        when(auth.issueToken("010", ActorContext.Role.ROLE_HEAD_ADMIN)).thenReturn(new IssuedToken("a", "r", at, at));

        AdminSignInResult result = service.execute(command);

        assertThat(result).isEqualTo(new AdminSignInResult("a", "r", at, at, ActorContext.Role.ROLE_HEAD_ADMIN));
    }

    @Test
    @DisplayName("it_일반 회원은 비밀번호 확인 전에 Forbidden 이다")
    void it_forbids_user_before_password_check() {
        given(ActorContext.Role.ROLE_USER, AccountStatus.PENDING, true);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(AdminForbiddenException.class);
        verify(auth, never()).passwordMatches(any(), any());
        verify(auth, never()).issueToken(any(), any());
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    @DisplayName("it_비밀번호가 틀리면 상태와 무관하게 Unauthorized 다")
    void it_hides_status_for_wrong_password(AccountStatus status) {
        given(ActorContext.Role.ROLE_PLACE_ADMIN, status, false);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(AdminUnauthorizedException.class);
        verify(auth, never()).issueToken(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"PENDING", "SUSPENDED", "WITHDRAWN"})
    @DisplayName("it_승인 대기와 제한 상태를 구분하고 토큰을 발급하지 않는다")
    void it_rejects_inactive_status(AccountStatus status) {
        given(ActorContext.Role.ROLE_PLACE_ADMIN, status, true);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(
                status == AccountStatus.PENDING ? AdminPendingApprovalException.class : AdminForbiddenException.class);
        verify(auth, never()).issueToken(any(), any());
    }
}
