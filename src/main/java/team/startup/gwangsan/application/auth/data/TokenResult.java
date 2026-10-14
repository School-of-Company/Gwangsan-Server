package team.startup.gwangsan.application.auth.data;

import java.time.LocalDateTime;

public record TokenResult(String accessToken, String refreshToken,
                          LocalDateTime accessTokenExpiresIn, LocalDateTime refreshTokenExpiresIn) {
}
