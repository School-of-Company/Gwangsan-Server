package team.startup.gwangsan.application.admin.data;

import team.startup.gwangsan.domain.common.ActorContext;

import java.time.LocalDateTime;

public record AdminSignInResult(
        String accessToken,
        String refreshToken,
        LocalDateTime accessTokenExpiresIn,
        LocalDateTime refreshTokenExpiresIn,
        ActorContext.Role role
) {
}
