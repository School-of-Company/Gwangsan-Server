package team.startup.gwangsan.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.auth.MemberDetails;
import team.startup.gwangsan.global.auth.MemberDetailsService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenParser 단위 테스트")
class TokenParserTest {

    private static final String ACCESS_SECRET = "access-secret-that-is-long-enough-for-hs256";
    private static final String REFRESH_SECRET = "refresh-secret-that-is-long-enough-for-hs256";
    private static final String PHONE_NUMBER = "010-1234-5678";

    @Mock private MemberRepository memberRepository;

    @Nested
    @DisplayName("resolveToken()은")
    class Describe_resolveToken {

        @Test
        @DisplayName("it_헤더가 없으면 null을 반환한다")
        void it_returns_null_when_header_is_absent() {
            assertThat(parser().resolveToken(new MockHttpServletRequest())).isNull();
        }

        @Test
        @DisplayName("it_Bearer 형식이 아닌 헤더면 null을 반환한다")
        void it_returns_null_when_header_is_malformed() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Token access-token");

            assertThat(parser().resolveToken(request)).isNull();
        }

        @Test
        @DisplayName("it_Bearer 헤더에서 토큰 값만 반환한다")
        void it_returns_token_when_header_is_bearer() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer access-token");

            assertThat(parser().resolveToken(request)).isEqualTo("access-token");
        }
    }

    @Nested
    @DisplayName("parseClaims()은")
    class Describe_parseClaims {

        @Test
        @DisplayName("it_유효한 서명 토큰의 주체를 반환한다")
        void it_returns_subject_from_valid_token() {
            Claims claims = parser().parseClaims(accessToken(PHONE_NUMBER, Instant.now().plusSeconds(60)));

            assertThat(claims.getSubject()).isEqualTo(PHONE_NUMBER);
            verifyNoInteractions(memberRepository);
        }

        @Test
        @DisplayName("it_만료 토큰에서도 포함된 주체를 반환한다")
        void it_returns_claims_from_expired_token() {
            Claims claims = parser().parseClaims(accessToken(PHONE_NUMBER, Instant.now().minusSeconds(60)));

            assertThat(claims.getSubject()).isEqualTo(PHONE_NUMBER);
            verifyNoInteractions(memberRepository);
        }
    }

    @Nested
    @DisplayName("getAuthentication()은")
    class Describe_getAuthentication {

        @Test
        @DisplayName("it_토큰 주체의 실제 MemberDetails를 principal로 반환한다")
        void it_returns_authentication_with_member_details() {
            Member member = member();
            when(memberRepository.findByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.of(member));

            Authentication authentication = parser().getAuthentication(accessToken(PHONE_NUMBER, Instant.now().plusSeconds(60)));

            assertThat(authentication.getPrincipal()).isInstanceOf(MemberDetails.class);
            MemberDetails principal = (MemberDetails) authentication.getPrincipal();
            assertThat(principal.getMember()).isSameAs(member);
            assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
            verify(memberRepository).findByPhoneNumber(PHONE_NUMBER);
        }
    }

    @Nested
    @DisplayName("parseRefreshToken()은")
    class Describe_parseRefreshToken {

        @Test
        @DisplayName("it_Bearer 형식이 아닌 값과 null에는 null을 반환한다")
        void it_returns_null_when_refresh_token_is_absent_or_malformed() {
            assertThat(parser().parseRefreshToken(null)).isNull();
            assertThat(parser().parseRefreshToken("Token refresh-token")).isNull();
        }

        @Test
        @DisplayName("it_Bearer 접두사를 제거해 반환한다")
        void it_returns_refresh_token_without_bearer_prefix() {
            assertThat(parser().parseRefreshToken("Bearer refresh-token")).isEqualTo("refresh-token");
        }
    }

    private TokenParser parser() {
        return new TokenParser(new MemberDetailsService(memberRepository), new JwtProperties(ACCESS_SECRET, REFRESH_SECRET));
    }

    private String accessToken(String subject, Instant expiration) {
        return Jwts.builder()
                .setSubject(subject)
                .setExpiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
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
