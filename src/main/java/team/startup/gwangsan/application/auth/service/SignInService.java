package team.startup.gwangsan.application.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.auth.data.IssuedToken;
import team.startup.gwangsan.application.auth.data.SignInCommand;
import team.startup.gwangsan.application.auth.data.TokenResult;
import team.startup.gwangsan.application.auth.port.in.SignInUseCase;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.DeviceTokenPort;
import team.startup.gwangsan.application.auth.port.out.PasswordHashPort;
import team.startup.gwangsan.application.auth.port.out.RefreshTokenPort;
import team.startup.gwangsan.application.auth.port.out.TokenPort;
import team.startup.gwangsan.domain.auth.exception.NotFoundUserException;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.model.Account;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SignInService implements SignInUseCase {

    private final AccountPort accountPort;
    private final PasswordHashPort passwordHashPort;
    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;
    private final DeviceTokenPort deviceTokenPort;

    @Override
    @Transactional
    public TokenResult execute(SignInCommand command) {
        Account account = accountPort.findByNickname(command.nickname())
                .orElseThrow(NotFoundUserException::new);
        // 비밀번호를 먼저 확인해 잘못된 자격증명에 계정 상태를 노출하지 않는다.
        if (!passwordHashPort.matches(command.password(), account.passwordHash())) {
            throw new UnauthorizedException();
        }
        account.assertCanSignIn();

        IssuedToken token = tokenPort.issue(account.phoneNumber(), account.role());
        refreshTokenPort.save(account.phoneNumber(), token.refreshToken());

        if (command.deviceId() != null && command.deviceToken() != null) {
            deviceTokenPort.register(command.deviceId(), account.id(), command.deviceToken(), command.osType());
        }
        return toResult(token);
    }

    static TokenResult toResult(IssuedToken token) {
        LocalDateTime now = LocalDateTime.now();
        return new TokenResult(token.accessToken(), token.refreshToken(),
                now.plusSeconds(token.accessTokenSeconds()), now.plusSeconds(token.refreshTokenSeconds()));
    }
}
