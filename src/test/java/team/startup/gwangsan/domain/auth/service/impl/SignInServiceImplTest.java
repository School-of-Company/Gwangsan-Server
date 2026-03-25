package team.startup.gwangsan.domain.auth.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.NotFoundUserException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignInRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
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

            @Test
            @DisplayName("ForbiddenException을 던진다")
            void it_throws_forbidden_exception() {
                Member member = mock(Member.class);
                when(member.getStatus()).thenReturn(MemberStatus.PENDING);
                SignInRequest request = new SignInRequest("테스터일", "pw", null, null, null);

                when(memberRepository.findByNickname("테스터일")).thenReturn(Optional.of(member));

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(ForbiddenException.class);
            }
        }

        @Nested
        @DisplayName("비밀번호가 일치하지 않을 때")
        class Context_with_wrong_password {

            @Test
            @DisplayName("UnauthorizedException을 던진다")
            void it_throws_unauthorized_exception() {
                Member member = activeMember();
                SignInRequest request = new SignInRequest("테스터일", "wrong", null, null, null);

                when(memberRepository.findByNickname("테스터일")).thenReturn(Optional.of(member));
                when(passwordEncoder.matches("wrong", "encodedPw")).thenReturn(false);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(UnauthorizedException.class);
            }
        }
    }
}
