package team.startup.gwangsan.global.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.auth.MemberDetails;
import team.startup.gwangsan.global.auth.MemberDetailsService;
import team.startup.gwangsan.global.security.jwt.JwtProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 단위 테스트")
class JwtAuthenticationFilterTest {

    private static final String PHONE_NUMBER = "010-1234-5678";

    @Mock private JwtProvider jwtProvider;
    @Mock private MemberRepository memberRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilter()는")
    class Describe_doFilter {

        @Test
        @DisplayName("it_Authorization 헤더가 없으면 인증 없이 다음 필터로 전달한다")
        void it_continues_without_authentication_when_header_is_absent() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter().doFilter(request, response, chain);

            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(chain.getResponse()).isSameAs(response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtProvider, memberRepository);
        }

        @Test
        @DisplayName("it_Bearer 형식이 아닌 헤더면 인증 없이 다음 필터로 전달한다")
        void it_continues_without_authentication_when_header_is_malformed() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
            request.addHeader("Authorization", "Token access-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter().doFilter(request, response, chain);

            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtProvider, memberRepository);
        }

        @Test
        @DisplayName("it_토큰 값이 비어 있는 Bearer 헤더면 인증 없이 다음 필터로 전달한다")
        void it_continues_without_authentication_when_bearer_token_is_empty() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
            request.addHeader("Authorization", "Bearer ");
            MockFilterChain chain = new MockFilterChain();

            filter().doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtProvider, memberRepository);
        }

        @Test
        @DisplayName("it_유효한 Bearer 토큰이면 실제 회원 상세 정보를 SecurityContext에 저장하고 다음 필터로 전달한다")
        void it_authenticates_with_member_details_when_bearer_token_is_valid() throws Exception {
            Member member = member();
            when(jwtProvider.validateAccessToken("access-token")).thenReturn(true);
            when(jwtProvider.validateAndGetSubject("access-token")).thenReturn(PHONE_NUMBER);
            when(memberRepository.findByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.of(member));
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
            request.addHeader("Authorization", "Bearer access-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter().doFilter(request, response, chain);

            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isInstanceOf(UsernamePasswordAuthenticationToken.class);
            MemberDetails principal = (MemberDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.getMember()).isSameAs(member);
            assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
            verify(jwtProvider).validateAccessToken("access-token");
            verify(jwtProvider).validateAndGetSubject("access-token");
            verify(memberRepository).findByPhoneNumber(PHONE_NUMBER);
        }

        @Test
        @DisplayName("it_검증에 실패한 Bearer 토큰이면 주체를 조회하지 않고 다음 필터로 전달한다")
        void it_does_not_load_a_principal_when_bearer_token_is_rejected() throws Exception {
            when(jwtProvider.validateAccessToken("bad-token")).thenReturn(false);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
            request.addHeader("Authorization", "Bearer bad-token");
            MockFilterChain chain = new MockFilterChain();

            filter().doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(jwtProvider).validateAccessToken("bad-token");
            verify(jwtProvider, never()).validateAndGetSubject("bad-token");
            verifyNoInteractions(memberRepository);
        }

        @Test
        @DisplayName("it_인증 제외 경로에서는 Bearer 헤더가 있어도 다음 필터로만 전달한다")
        void it_skips_authentication_for_excluded_path() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/sign-in");
            request.addHeader("Authorization", "Bearer access-token");
            MockFilterChain chain = new MockFilterChain();

            filter().doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtProvider, memberRepository);
        }
    }

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(jwtProvider, new MemberDetailsService(memberRepository));
    }

    private Member member() {
        return Member.builder()
                .name("회원")
                .nickname("회원닉네임")
                .phoneNumber(PHONE_NUMBER)
                .password("password")
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
