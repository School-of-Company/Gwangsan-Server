package team.startup.gwangsan.application.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.auth.data.IssuedToken;
import team.startup.gwangsan.application.auth.port.in.IssueAccountTokenUseCase;
import team.startup.gwangsan.application.auth.port.in.PasswordMatchQuery;
import team.startup.gwangsan.application.auth.port.out.PasswordHashPort;
import team.startup.gwangsan.application.auth.port.out.RefreshTokenPort;
import team.startup.gwangsan.application.auth.port.out.TokenPort;

@Service
@RequiredArgsConstructor
public class AccountTokenService implements IssueAccountTokenUseCase, PasswordMatchQuery {

    private final PasswordHashPort passwordHashPort;
    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    public IssuedToken execute(String phoneNumber, String role) {
        IssuedToken token = tokenPort.issue(phoneNumber, role);
        refreshTokenPort.save(phoneNumber, token.refreshToken());
        return token;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordHashPort.matches(rawPassword, passwordHash);
    }
}
