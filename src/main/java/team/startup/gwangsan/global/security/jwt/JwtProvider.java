package team.startup.gwangsan.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.global.security.exception.ExpiredTokenException;
import team.startup.gwangsan.global.security.exception.InvalidTokenException;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final long ACCESS_TOKEN_TIME = 60L * 60 * 24;         // 1일
    public static final long REFRESH_TOKEN_TIME = 60L * 60 * 24 * 7;     // 7일

    private final JwtProperties jwtProperties;

    private Key getAccessKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
    }

    private Key getRefreshKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getAccessKey()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        } catch (Exception e) {
            throw new InvalidTokenException();
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getRefreshKey()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        } catch (Exception e) {
            throw new InvalidTokenException();
        }
    }

    public String validateAndGetSubject(String token) {
        if (!validateAccessToken(token)) {
            throw new InvalidTokenException();
        }
        return Jwts.parserBuilder()
                .setSigningKey(getAccessKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String generateAccessToken(String phoneNumber, MemberRole role) {
        Date expiry = new Date(System.currentTimeMillis() + ACCESS_TOKEN_TIME * 1000);

        return Jwts.builder()
                .setSubject(phoneNumber)
                .claim(AUTHORITIES_KEY, "ROLE_" + role.name())
                .setIssuedAt(new Date())
                .setExpiration(expiry)
                .signWith(getAccessKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String phoneNumber) {
        Date expiry = new Date(System.currentTimeMillis() + REFRESH_TOKEN_TIME * 1000);

        return Jwts.builder()
                .setSubject(phoneNumber)
                .setExpiration(expiry)
                .signWith(getRefreshKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public long getAccessTokenTime() {
        return ACCESS_TOKEN_TIME;
    }

    public long getRefreshTokenTime() {
        return REFRESH_TOKEN_TIME;
    }
}


