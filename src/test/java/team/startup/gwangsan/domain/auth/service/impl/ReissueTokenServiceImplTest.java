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
import team.startup.gwangsan.domain.auth.entity.RefreshToken;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.NotFoundUserException;
import team.startup.gwangsan.domain.auth.exception.PendingApprovalException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.security.jwt.JwtProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReissueTokenServiceImpl 단위 테스트")
class ReissueTokenServiceImplTest {

    @InjectMocks
    private ReissueTokenServiceImpl service;

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private MemberRepository memberRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("유효한 refresh token으로 재발급 시")
        class Context_with_valid_token {

            @Test
            @DisplayName("새 access/refresh 토큰을 반환한다")
            void it_returns_new_tokens() {
                RefreshToken savedToken = mock(RefreshToken.class);
                when(savedToken.getPhoneNumber()).thenReturn("01012345678");

                Member member = mock(Member.class);
                when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
                when(member.getPhoneNumber()).thenReturn("01012345678");
                when(member.getRole()).thenReturn(MemberRole.ROLE_USER);

                when(refreshTokenRepository.findByToken("oldToken")).thenReturn(Optional.of(savedToken));
                when(jwtProvider.validateRefreshToken("oldToken")).thenReturn(true);
                when(memberRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.of(member));
                when(jwtProvider.generateAccessToken(any(), any())).thenReturn("newAccess");
                when(jwtProvider.generateRefreshToken(any())).thenReturn("newRefresh");
                when(jwtProvider.getAccessTokenTime()).thenReturn(3600L);
                when(jwtProvider.getRefreshTokenTime()).thenReturn(86400L);

                TokenResponse response = service.execute("oldToken");

                assertThat(response.accessToken()).isEqualTo("newAccess");
                assertThat(response.refreshToken()).isEqualTo("newRefresh");
                verify(refreshTokenRepository).save(any(RefreshToken.class));
            }
        }

        @Nested
        @DisplayName("저장된 refresh token이 없을 때")
        class Context_with_token_not_found {

            @Test
            @DisplayName("UnauthorizedException을 던진다")
            void it_throws_unauthorized_exception() {
                when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute("invalid"))
                        .isInstanceOf(UnauthorizedException.class);
                verifyNoInteractions(memberRepository);
                verify(refreshTokenRepository, never()).save(any());
                verify(jwtProvider, never()).generateAccessToken(any(), any());
                verify(jwtProvider, never()).generateRefreshToken(any());
            }
        }

        @Nested
        @DisplayName("refresh token 서명이 유효하지 않을 때")
        class Context_with_invalid_signature {

            @Test
            @DisplayName("UnauthorizedException을 던진다")
            void it_throws_unauthorized_exception() {
                RefreshToken savedToken = mock(RefreshToken.class);
                when(refreshTokenRepository.findByToken("badToken")).thenReturn(Optional.of(savedToken));
                when(jwtProvider.validateRefreshToken("badToken")).thenReturn(false);

                assertThatThrownBy(() -> service.execute("badToken"))
                        .isInstanceOf(UnauthorizedException.class);
                verifyNoInteractions(memberRepository);
                verify(refreshTokenRepository, never()).save(any());
                verify(jwtProvider, never()).generateAccessToken(any(), any());
                verify(jwtProvider, never()).generateRefreshToken(any());
            }
        }

        @Nested
        @DisplayName("회원이 존재하지 않을 때")
        class Context_with_member_not_found {

            @Test
            @DisplayName("NotFoundUserException을 던진다")
            void it_throws_not_found_user_exception() {
                RefreshToken savedToken = mock(RefreshToken.class);
                when(savedToken.getPhoneNumber()).thenReturn("01099999999");

                when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(savedToken));
                when(jwtProvider.validateRefreshToken("token")).thenReturn(true);
                when(memberRepository.findByPhoneNumber("01099999999")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute("token"))
                        .isInstanceOf(NotFoundUserException.class);
            }
        }

        @Nested
        @DisplayName("비활성 상태의 회원일 때")
        class Context_with_inactive_member {

            @ParameterizedTest
            @EnumSource(value = MemberStatus.class, names = {"PENDING", "SUSPENDED", "WITHDRAWN"})
            @DisplayName("승인 대기와 제한 상태를 구분하고 토큰을 발급하지 않는다")
            void it_rejects_inactive_status(MemberStatus status) {
                RefreshToken savedToken = mock(RefreshToken.class);
                when(savedToken.getPhoneNumber()).thenReturn("01012345678");

                Member member = mock(Member.class);
                when(member.getStatus()).thenReturn(status);

                when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(savedToken));
                when(jwtProvider.validateRefreshToken("token")).thenReturn(true);
                when(memberRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.of(member));

                assertThatThrownBy(() -> service.execute("token"))
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
            }
        }
    }
}
