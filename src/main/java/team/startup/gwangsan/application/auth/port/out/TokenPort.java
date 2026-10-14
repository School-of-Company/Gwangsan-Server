package team.startup.gwangsan.application.auth.port.out;

import team.startup.gwangsan.application.auth.data.IssuedToken;

public interface TokenPort {

    IssuedToken issue(String phoneNumber, String role);

    /** 서명·블랙리스트 검사. 만료/위조는 기존 토큰 예외를 그대로 던진다. */
    boolean isValidRefreshToken(String refreshToken);

    long remainingMillis(String accessToken);
}
