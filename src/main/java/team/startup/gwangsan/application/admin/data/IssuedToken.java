package team.startup.gwangsan.application.admin.data;

import java.time.LocalDateTime;

public record IssuedToken(
        String accessToken,
        String refreshToken,
        LocalDateTime accessTokenExpiresIn,
        LocalDateTime refreshTokenExpiresIn
) {
}
