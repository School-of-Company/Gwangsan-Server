package team.startup.gwangsan.application.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.auth.data.TokenResult;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.RefreshTokenPort;
import team.startup.gwangsan.application.auth.port.out.TokenPort;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.NotFoundUserException;
import team.startup.gwangsan.domain.auth.exception.PendingApprovalException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.model.AccountStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.PHONE;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.account;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.token;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReissueTokenService 단위 테스트")
class ReissueTokenServiceTest {

    @Mock private RefreshTokenPort refreshTokenPort;
    @Mock private TokenPort tokenPort;
    @Mock private AccountPort accountPort;

    private ReissueTokenService service;

    @BeforeEach
    void setUp() {
        service = new ReissueTokenService(refreshTokenPort, tokenPort, accountPort);
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("유효한 refresh token으로 재발급 시")
        class Context_with_valid_token {

            @Test
            @DisplayName("새 토큰을 반환하고 같은 번호로 refresh token을 덮어쓴다")
            void it_returns_new_tokens() {
                when(refreshTokenPort.findPhoneNumberByToken("oldToken")).thenReturn(Optional.of(PHONE));
                when(tokenPort.isValidRefreshToken("oldToken")).thenReturn(true);
                when(accountPort.findByPhoneNumber(PHONE)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
                when(tokenPort.issue(PHONE, "ROLE_USER")).thenReturn(token("newAccess", "newRefresh"));

                TokenResult result = service.execute("oldToken");

                assertThat(result.accessToken()).isEqualTo("newAccess");
                assertThat(result.refreshToken()).isEqualTo("newRefresh");
                verify(refreshTokenPort).save(PHONE, "newRefresh");
            }
        }

        @Nested
        @DisplayName("저장된 refresh token이 없을 때")
        class Context_with_token_not_found {

            @Test
            @DisplayName("UnauthorizedException을 던진다")
            void it_throws_unauthorized_exception() {
                when(refreshTokenPort.findPhoneNumberByToken("invalid")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute("invalid")).isInstanceOf(UnauthorizedException.class);
                verifyNoInteractions(accountPort, tokenPort);
                verify(refreshTokenPort, never()).save(any(), any());
            }
        }

        @Nested
        @DisplayName("refresh token 서명이 유효하지 않을 때")
        class Context_with_invalid_signature {

            @Test
            @DisplayName("회원 조회 없이 UnauthorizedException을 던진다")
            void it_throws_unauthorized_exception() {
                when(refreshTokenPort.findPhoneNumberByToken("badToken")).thenReturn(Optional.of(PHONE));
                when(tokenPort.isValidRefreshToken("badToken")).thenReturn(false);

                assertThatThrownBy(() -> service.execute("badToken")).isInstanceOf(UnauthorizedException.class);
                verifyNoInteractions(accountPort);
                verify(tokenPort, never()).issue(any(), any());
                verify(refreshTokenPort, never()).save(any(), any());
            }
        }

        @Nested
        @DisplayName("회원이 존재하지 않을 때")
        class Context_with_member_not_found {

            @Test
            @DisplayName("NotFoundUserException을 던진다")
            void it_throws_not_found_user_exception() {
                when(refreshTokenPort.findPhoneNumberByToken("token")).thenReturn(Optional.of("01099999999"));
                when(tokenPort.isValidRefreshToken("token")).thenReturn(true);
                when(accountPort.findByPhoneNumber("01099999999")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute("token")).isInstanceOf(NotFoundUserException.class);
            }
        }

        @Nested
        @DisplayName("비활성 상태의 회원일 때")
        class Context_with_inactive_member {

            @ParameterizedTest
            @EnumSource(value = AccountStatus.class, names = {"PENDING", "SUSPENDED", "WITHDRAWN"})
            @DisplayName("승인 대기와 제한 상태를 구분하고 토큰을 발급하지 않는다")
            void it_rejects_inactive_status(AccountStatus status) {
                when(refreshTokenPort.findPhoneNumberByToken("token")).thenReturn(Optional.of(PHONE));
                when(tokenPort.isValidRefreshToken("token")).thenReturn(true);
                when(accountPort.findByPhoneNumber(PHONE)).thenReturn(Optional.of(account(status)));

                assertThatThrownBy(() -> service.execute("token"))
                        .isExactlyInstanceOf(status == AccountStatus.PENDING
                                ? PendingApprovalException.class : ForbiddenException.class);
                verify(tokenPort, never()).issue(any(), any());
                verify(refreshTokenPort, never()).save(any(), any());
            }
        }
    }
}
