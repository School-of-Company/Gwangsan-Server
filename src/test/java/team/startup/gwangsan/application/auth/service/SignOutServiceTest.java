package team.startup.gwangsan.application.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.RefreshTokenPort;
import team.startup.gwangsan.application.auth.port.out.TokenBlacklistPort;
import team.startup.gwangsan.application.auth.port.out.TokenPort;
import team.startup.gwangsan.domain.auth.exception.NotFoundMemberAccountException;
import team.startup.gwangsan.domain.auth.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.PHONE;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.account;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignOutService 단위 테스트")
class SignOutServiceTest {

    private static final ActorContext ACTOR = new ActorContext(1L, ActorContext.Role.ROLE_USER);

    @Mock private AccountPort accountPort;
    @Mock private RefreshTokenPort refreshTokenPort;
    @Mock private TokenPort tokenPort;
    @Mock private TokenBlacklistPort tokenBlacklistPort;

    private SignOutService service;

    @BeforeEach
    void setUp() {
        service = new SignOutService(accountPort, refreshTokenPort, tokenPort, tokenBlacklistPort);
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("refresh token을 삭제하고 access token을 남은 만료시간 동안 블랙리스트에 등록한다")
        void it_deletes_refresh_token_and_blacklists_access_token() {
            when(accountPort.findById(1L)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
            when(tokenPort.remainingMillis("accessToken")).thenReturn(300000L);

            service.execute(ACTOR, "accessToken");

            verify(refreshTokenPort).deleteByPhoneNumber(PHONE);
            verify(tokenBlacklistPort).add("accessToken", 300000L);
        }

        @Test
        @DisplayName("실행자 회원이 없으면 아무 토큰도 건드리지 않고 NotFoundMemberAccountException을 던진다")
        void it_throws_when_actor_is_missing() {
            when(accountPort.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ACTOR, "accessToken"))
                    .isInstanceOf(NotFoundMemberAccountException.class);
            verifyNoInteractions(refreshTokenPort, tokenBlacklistPort);
        }
    }
}
