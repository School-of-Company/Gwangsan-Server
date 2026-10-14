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
import team.startup.gwangsan.application.auth.data.SignInCommand;
import team.startup.gwangsan.application.auth.data.TokenResult;
import team.startup.gwangsan.application.auth.port.out.*;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.NotFoundUserException;
import team.startup.gwangsan.domain.auth.exception.PendingApprovalException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.model.AccountStatus;
import team.startup.gwangsan.global.exception.GlobalException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.PHONE;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.account;
import static team.startup.gwangsan.application.auth.service.AuthPortFixture.token;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInService 단위 테스트")
class SignInServiceTest {

    @Mock private AccountPort accountPort;
    @Mock private PasswordHashPort passwordHashPort;
    @Mock private TokenPort tokenPort;
    @Mock private RefreshTokenPort refreshTokenPort;
    @Mock private DeviceTokenPort deviceTokenPort;

    private SignInService service;

    @BeforeEach
    void setUp() {
        service = new SignInService(accountPort, passwordHashPort, tokenPort, refreshTokenPort, deviceTokenPort);
    }

    private void activeMemberSignsIn() {
        when(accountPort.findByNickname("테스터일")).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));
        when(passwordHashPort.matches("pw", "encodedPw")).thenReturn(true);
        when(tokenPort.issue(PHONE, "ROLE_USER")).thenReturn(token("accessToken", "refreshToken"));
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("deviceToken이 있는 정상 로그인 시")
        class Context_with_device_token {

            @Test
            @DisplayName("토큰을 반환하고 refresh token과 DeviceToken을 저장한다")
            void it_returns_token_and_saves_device_token() {
                activeMemberSignsIn();

                TokenResult result = service.execute(new SignInCommand("테스터일", "pw", "token", "device-001", "IOS"));

                assertThat(result.accessToken()).isEqualTo("accessToken");
                assertThat(result.refreshToken()).isEqualTo("refreshToken");
                assertThat(result.accessTokenExpiresIn()).isCloseTo(LocalDateTime.now().plusSeconds(3600), within(5, java.time.temporal.ChronoUnit.SECONDS));
                assertThat(result.refreshTokenExpiresIn()).isCloseTo(LocalDateTime.now().plusSeconds(86400), within(5, java.time.temporal.ChronoUnit.SECONDS));
                verify(refreshTokenPort).save(PHONE, "refreshToken");
                verify(deviceTokenPort).register("device-001", 1L, "token", "IOS");
            }
        }

        @Nested
        @DisplayName("deviceToken이 없는 정상 로그인 시")
        class Context_without_device_token {

            @Test
            @DisplayName("토큰을 반환하고 DeviceToken을 저장하지 않는다")
            void it_returns_token_without_saving_device_token() {
                activeMemberSignsIn();

                TokenResult result = service.execute(new SignInCommand("테스터일", "pw", null, null, null));

                assertThat(result.accessToken()).isEqualTo("accessToken");
                verifyNoInteractions(deviceTokenPort);
            }
        }

        @Nested
        @DisplayName("deviceId만 있는 정상 로그인 시")
        class Context_with_device_id_without_device_token {

            @Test
            @DisplayName("DeviceToken을 저장하지 않는다")
            void it_does_not_save_device_token() {
                activeMemberSignsIn();

                service.execute(new SignInCommand("테스터일", "pw", null, "device-001", null));

                verifyNoInteractions(deviceTokenPort);
            }
        }

        @Nested
        @DisplayName("닉네임에 해당하는 회원이 없을 때")
        class Context_with_user_not_found {

            @Test
            @DisplayName("NotFoundUserException을 던진다")
            void it_throws_not_found_user_exception() {
                when(accountPort.findByNickname("없는닉네임")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(new SignInCommand("없는닉네임", "pw", null, null, null)))
                        .isInstanceOf(NotFoundUserException.class);
            }
        }

        @Nested
        @DisplayName("비활성 상태의 회원이 로그인 시도할 때")
        class Context_with_inactive_member {

            @ParameterizedTest
            @EnumSource(value = AccountStatus.class, names = {"PENDING", "SUSPENDED", "WITHDRAWN"})
            @DisplayName("승인 대기와 제한 상태를 구분하고 토큰을 발급하지 않는다")
            void it_rejects_inactive_status(AccountStatus status) {
                when(accountPort.findByNickname("테스터일")).thenReturn(Optional.of(account(status)));
                when(passwordHashPort.matches("pw", "encodedPw")).thenReturn(true);

                assertThatThrownBy(() -> service.execute(new SignInCommand("테스터일", "pw", "t", "d", null)))
                        .isExactlyInstanceOf(status == AccountStatus.PENDING
                                ? PendingApprovalException.class : ForbiddenException.class)
                        .isInstanceOfSatisfying(GlobalException.class, exception -> {
                            assertThat(exception.getErrorCode().getStatus()).isEqualTo(403);
                            assertThat(exception.getErrorCode().getMessage()).isEqualTo(
                                    status == AccountStatus.PENDING
                                            ? "승인 대기 중인 계정입니다. 관리자 승인 후 이용 가능합니다."
                                            : "탈퇴한 회원이거나 접근이 제한된 계정입니다.");
                        });
                verifyNoInteractions(tokenPort, refreshTokenPort, deviceTokenPort);
            }
        }

        @Nested
        @DisplayName("비밀번호가 일치하지 않을 때")
        class Context_with_wrong_password {

            @ParameterizedTest
            @EnumSource(AccountStatus.class)
            @DisplayName("상태와 관계없이 UnauthorizedException을 던진다")
            void it_throws_unauthorized_exception(AccountStatus status) {
                when(accountPort.findByNickname("테스터일")).thenReturn(Optional.of(account(status)));
                when(passwordHashPort.matches("wrong", "encodedPw")).thenReturn(false);

                assertThatThrownBy(() -> service.execute(new SignInCommand("테스터일", "wrong", null, null, null)))
                        .isExactlyInstanceOf(UnauthorizedException.class);
                verifyNoInteractions(tokenPort, refreshTokenPort, deviceTokenPort);
            }
        }
    }
}
