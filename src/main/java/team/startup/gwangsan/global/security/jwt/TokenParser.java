package team.startup.gwangsan.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import team.startup.gwangsan.global.auth.MemberDetailsService;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import static team.startup.gwangsan.global.filter.JwtFilter.AUTHORIZATION_HEADER;
import static team.startup.gwangsan.global.filter.JwtFilter.BEARER_PREFIX;

@Component
@RequiredArgsConstructor
public class TokenParser {

    private static final String BEARER_TYPE = "Bearer ";

    private final MemberDetailsService memberDetailsService;
    private final JwtProperties jwtProperties;

    private Key getAccessKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getAccessKey())
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);
        UserDetails principal = memberDetailsService.loadUserByUsername(claims.getSubject());
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    public String parseRefreshToken(String refreshToken) {
        if (refreshToken != null && refreshToken.startsWith(BEARER_TYPE)) {
            return refreshToken.substring(BEARER_TYPE.length());
        }
        return null;
    }
}
