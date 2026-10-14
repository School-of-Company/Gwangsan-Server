package team.startup.gwangsan.adapter.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.data.IssuedToken;
import team.startup.gwangsan.application.auth.port.out.TokenPort;
import team.startup.gwangsan.domain.member.model.MemberRole;

@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements TokenPort {

    private final JwtProvider jwtProvider;

    @Override
    public IssuedToken issue(String phoneNumber, String role) {
        return new IssuedToken(
                jwtProvider.generateAccessToken(phoneNumber, MemberRole.valueOf(role)),
                jwtProvider.generateRefreshToken(phoneNumber),
                jwtProvider.getAccessTokenTime(),
                jwtProvider.getRefreshTokenTime());
    }

    @Override
    public boolean isValidRefreshToken(String refreshToken) {
        return jwtProvider.validateRefreshToken(refreshToken);
    }

    @Override
    public long remainingMillis(String accessToken) {
        return jwtProvider.getExpiration(accessToken);
    }
}
