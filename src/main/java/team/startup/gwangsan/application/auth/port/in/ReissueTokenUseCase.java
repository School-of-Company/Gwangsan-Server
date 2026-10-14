package team.startup.gwangsan.application.auth.port.in;

import team.startup.gwangsan.application.auth.data.TokenResult;

public interface ReissueTokenUseCase {

    TokenResult execute(String refreshToken);
}
