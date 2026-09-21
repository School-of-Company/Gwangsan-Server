package team.startup.gwangsan.domain.auth.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.NotFoundUserException;
import team.startup.gwangsan.domain.auth.exception.PendingApprovalException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignInRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.security.jwt.JwtProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInServiceImpl 단위 테스트")
class SignInServiceImplTest {

    @InjectMocks
    private SignInServiceImpl service;

    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;

    private Member activeMember() {
        Member member = mock(Member.class);
        when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
        when(member.getPassword()).thenReturn("encodedPw");
        return member;
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("deviceToken이 있는 정상 로그인 시")
        class Context_with_device_token {

            @Test
            @DisplayName("토큰을 반환하고 DeviceToken을 저장한다")
            void it_returns_token_and_saves_device_token() {
                Member member = activeMember();
                when(member.getPhoneNumber()).thenReturn("01012345678");
                when(member.getRole()).thenReturn(MemberRole.ROLE_USER);
                SignInRequest request = new SignInRequest("테스터일", "pw", "token", "device-001", null);

                when(memberRepository.findByNickname("테스터일")).thenReturn(Optional.of(member));
                when(passwordEncoder.matches("pw", "encodedPw")).thenReturn(true);
                when(jwtProvider.generateAccessToken(any(), any())).thenReturn("accessToken");
                when(jwtProvider.generateRefreshToken(any())).thenReturn("refreshToken");
                when(jwtProvider.getAccessTokenTime()).thenReturn(3600L);
                when(jwtProvider.getRefreshTokenTime()).thenReturn(86400L);

                TokenResponse response = service.execute(request);

                assertThat(response.accessToken()).isEqualTo("accessToken");
                assertThat(response.refreshToken()).isEqualTo("refreshToken");
                verify(deviceTokenRepository).save(any());
            }
        }

        @Nested
        @DisplayName("deviceToken이 없는 정상 로그인 시")
        class Context_without_device_token {

            @Test
            @DisplayName("토큰을 반환하고 DeviceToken을 저장하지 않는다")
            void it_returns_token_without_saving_device_token() {
                Member member = activeMember();
                when(member.getPhoneNumber()).thenReturn("01012345678");
                when(member.getRole()).thenReturn(MemberRole.ROLE_USER);
                SignInRequest request = new SignInRequest("테스터일", "pw", null, null, null);

                when(memberRepository.findByNickname("테스터일")).thenReturn(Optional.of(member));
                when(passwordEncoder.matches("pw", "encodedPw")).thenReturn(true);
                when(jwtProvider.generateAccessToken(any(), any())).thenReturn("accessToken");
                when(jwtProvider.generateRefreshToken(any())).thenReturn("refreshToken");
                when(jwtProvider.getAccessTokenTime()).thenReturn(3600L);
                when(jwtProvider.getRefreshTokenTime()).thenReturn(86400L);

                TokenResponse response = service.execute(request);

                assertThat(response.accessToken()).isEqualTo("accessToken");
                verify(deviceTokenRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("deviceId만 있는 정상 로그인 시")
        class Context_with_device_id_without_device_token {

            @Test
            @DisplayName("DeviceToken을 저장하지 않는다")
            void it_does_not_save_device_token() {
                Member member = activeMember();
                when(member.getPhoneNumber()).thenReturn("01012345678");
                when(member.getRole()).thenReturn(MemberRole.ROLE_USER);
                SignInRequest request = new SignInRequest("테스터일", "pw", null, "device-001", null);

                when(memberRepository.findByNickname("테스터일")).thenReturn(Optional.of(member));
                when(passwordEncoder.matches("pw", "encodedPw")).thenReturn(true);
                when(jwtProvider.generateAccessToken(any(), any())).thenReturn("accessToken");
                when(jwtProvider.generateRefreshToken(any())).thenReturn("refreshToken");
                when(jwtProvider.getAccessTokenTime()).thenReturn(3600L);
                when(jwtProvider.getRefreshTokenTime()).thenReturn(86400L);

                TokenResponse response = service.execute(request);

                assertThat(response.accessToken()).isEqualTo("accessToken");
                verify(deviceTokenRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("닉네임에 해당하는 회원이 없을 때")
        class Context_with_user_not_found {

            @Test
            @DisplayName("NotFoundUserException을 던진다")
            void it_throws_not_found_user_exception() {
                SignInRequest request = new SignInRequest("없는닉네임", "pw", null, null, null);
                when(memberRepository.findByNickname("없는닉네임")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(NotFoundUserException.class);
            }
        }

        @Nested
        @DisplayName("비활성 상태의 회원이 로그인 시도할 때")
        class Context_with_inactive_member {

            @ParameterizedTest
            @EnumSource(value = MemberStatus.class, names = {"PENDING", "SUSPENDED", "WITHDRAWN"})
            @DisplayName("승인 대기와 제한 상태를 구분하고 토큰을 발급하지 않는다")
            void it_rejects_inactive_status(MemberStatus status) {
                Member member = mock(Member.class);
                when(member.getStatus()).thenReturn(status);
                when(member.getPassword()).thenReturn("encodedPw");
                when(passwordEncoder.matches("pw", "encodedPw")).thenReturn(true);
                SignInRequest request = new SignInRequest("테스터일", "pw", null, null, null);

                when(memberRepository.findByNickname("테스터일")).thenReturn(Optional.of(member));

                assertThatThrownBy(() -> service.execute(request))
                        .isExactlyInstanceOf(status == MemberStatus.PENDING
                                ? PendingApprovalException.class : ForbiddenException.class)
                        .isInstanceOfSatisfying(GlobalException.class, exception -> {
                            assertThat(exception.getErrorCode().getStatus()).isEqualTo(403);
                            assertThat(exception.getErrorCode().getMessage()).isEqualTo(
                                    status == MemberStatus.PENDING
                                            ? "승인 대기 중인 계정입니다. 관리자 승인 후 이용 가능합니다."
                                            : "탈퇴한 회원이거나 접근이 제한된 계정입니다.");
                        });
                verify(jwtProvider, never()).generateAccessToken(any(), any());
                verify(jwtProvider, never()).generateRefreshToken(any());
                verify(refreshTokenRepository, never()).save(any());
                verifyNoInteractions(deviceTokenRepository);
            }
        }

        @Nested
        @DisplayName("비밀번호가 일치하지 않을 때")
        class Context_with_wrong_password {

            @ParameterizedTest
            @EnumSource(MemberStatus.class)
            @DisplayName("UnauthorizedException을 던진다")
            void it_throws_unauthorized_exception(MemberStatus status) {
                Member member = mock(Member.class);
                when(member.getPassword()).thenReturn("encodedPw");
                SignInRequest request = new SignInRequest("테스터일", "wrong", null, null, null);

                lenient().when(member.getStatus()).thenReturn(status);
                when(memberRepository.findByNickname("테스터일")).thenReturn(Optional.of(member));
                when(passwordEncoder.matches("wrong", "encodedPw")).thenReturn(false);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(UnauthorizedException.class);
                verify(member, never()).getStatus();
                verifyNoInteractions(jwtProvider, refreshTokenRepository);
            }
        }
    }
}
