package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.presentation.dto.response.SignInAdminResponse;
import team.startup.gwangsan.domain.auth.entity.RefreshToken;
import team.startup.gwangsan.domain.auth.exception.ForbiddenException;
import team.startup.gwangsan.domain.auth.exception.PendingApprovalException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.security.jwt.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInAdminServiceImpl 단위 테스트")
class SignInAdminServiceImplTest {

    @InjectMocks
    private SignInAdminServiceImpl service;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("관리자 정보가 유효할 때")
        class Context_with_valid_admin {

            @Test
            @DisplayName("토큰을 포함한 SignInAdminResponse를 반환한다")
            void it_returns_sign_in_response() {
                Member member = mock(Member.class);
                when(member.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);
                when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
                when(member.getPassword()).thenReturn("encodedPw");
                when(member.getPhoneNumber()).thenReturn("010-0000-0000");

                when(memberRepository.findByNickname("admin")).thenReturn(Optional.of(member));
                when(passwordEncoder.matches("pw", "encodedPw")).thenReturn(true);
                when(jwtProvider.generateAccessToken(any(), any())).thenReturn("accessToken");
                when(jwtProvider.generateRefreshToken(any())).thenReturn("refreshToken");
                when(jwtProvider.getAccessTokenTime()).thenReturn(3600L);
                when(jwtProvider.getRefreshTokenTime()).thenReturn(86400L);

                SignInAdminResponse response = service.execute("admin", "pw");

                assertThat(response).isNotNull();
                assertThat(response.token().accessToken()).isEqualTo("accessToken");
                assertThat(response.token().refreshToken()).isEqualTo("refreshToken");

                ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
                verify(refreshTokenRepository).save(captor.capture());
                assertThat(captor.getValue().getToken()).isEqualTo("refreshToken");
            }
        }

        @Nested
        @DisplayName("닉네임에 해당하는 회원이 없을 때")
        class Context_with_member_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                when(memberRepository.findByNickname("admin")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute("admin", "pw"))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }

        @Nested
        @DisplayName("ROLE_USER인 회원이 로그인 시도할 때")
        class Context_with_role_user {

            @Test
            @DisplayName("ForbiddenException을 던진다")
            void it_throws_forbidden_exception() {
                Member member = mock(Member.class);
                when(member.getRole()).thenReturn(MemberRole.ROLE_USER);
                when(memberRepository.findByNickname("user")).thenReturn(Optional.of(member));

                assertThatThrownBy(() -> service.execute("user", "pw"))
                        .isInstanceOf(ForbiddenException.class);
            }
        }

        @Nested
        @DisplayName("비활성 상태의 관리자가 로그인 시도할 때")
        class Context_with_inactive_admin {

            @ParameterizedTest
            @EnumSource(value = MemberStatus.class, names = {"PENDING", "SUSPENDED", "WITHDRAWN"})
            @DisplayName("승인 대기와 제한 상태를 구분하고 토큰을 발급하지 않는다")
            void it_rejects_inactive_status(MemberStatus status) {
                Member member = mock(Member.class);
                when(member.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);
                when(member.getStatus()).thenReturn(status);
                when(member.getPassword()).thenReturn("encodedPw");
                when(passwordEncoder.matches("pw", "encodedPw")).thenReturn(true);
                when(memberRepository.findByNickname("admin")).thenReturn(Optional.of(member));

                assertThatThrownBy(() -> service.execute("admin", "pw"))
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

        @Nested
        @DisplayName("비밀번호가 일치하지 않을 때")
        class Context_with_wrong_password {

            @ParameterizedTest
            @EnumSource(MemberStatus.class)
            @DisplayName("UnauthorizedException을 던진다")
            void it_throws_unauthorized_exception(MemberStatus status) {
                Member member = mock(Member.class);
                when(member.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);
                when(member.getPassword()).thenReturn("encodedPw");
                lenient().when(member.getStatus()).thenReturn(status);
                when(memberRepository.findByNickname("admin")).thenReturn(Optional.of(member));
                when(passwordEncoder.matches("wrong", "encodedPw")).thenReturn(false);

                assertThatThrownBy(() -> service.execute("admin", "wrong"))
                        .isInstanceOf(UnauthorizedException.class);
                verify(member, never()).getStatus();
                verifyNoInteractions(jwtProvider, refreshTokenRepository);
            }
        }
    }
}
