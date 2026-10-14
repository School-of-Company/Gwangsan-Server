package team.startup.gwangsan.application.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.auth.data.IssuedToken;
import team.startup.gwangsan.application.auth.data.TokenResult;
import team.startup.gwangsan.application.auth.port.in.ReissueTokenUseCase;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.RefreshTokenPort;
import team.startup.gwangsan.application.auth.port.out.TokenPort;
import team.startup.gwangsan.domain.auth.exception.NotFoundUserException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.model.Account;

@Service
@RequiredArgsConstructor
public class ReissueTokenService implements ReissueTokenUseCase {

    private final RefreshTokenPort refreshTokenPort;
    private final TokenPort tokenPort;
    private final AccountPort accountPort;

    @Override
    @Transactional
    public TokenResult execute(String refreshToken) {
        String phoneNumber = refreshTokenPort.findPhoneNumberByToken(refreshToken)
                .orElseThrow(UnauthorizedException::new);
        if (!tokenPort.isValidRefreshToken(refreshToken)) {
            throw new UnauthorizedException();
        }
        Account account = accountPort.findByPhoneNumber(phoneNumber)
                .orElseThrow(NotFoundUserException::new);
        account.assertCanSignIn();

        IssuedToken token = tokenPort.issue(account.phoneNumber(), account.role());
        // 같은 번호 키로 덮어써 이전 refresh token 을 무효화한다.
        refreshTokenPort.save(phoneNumber, token.refreshToken());
        return SignInService.toResult(token);
    }
}
