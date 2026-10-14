package team.startup.gwangsan.application.auth.data;

public record IssuedToken(String accessToken, String refreshToken, long accessTokenSeconds, long refreshTokenSeconds) {
}
