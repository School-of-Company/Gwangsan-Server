package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.AdminCredential;
import team.startup.gwangsan.application.admin.data.AdminSignInCommand;
import team.startup.gwangsan.application.admin.data.AdminSignInResult;
import team.startup.gwangsan.application.admin.data.IssuedToken;
import team.startup.gwangsan.application.admin.port.in.SignInAdminUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminAuthPort;
import team.startup.gwangsan.domain.admin.exception.AdminForbiddenException;
import team.startup.gwangsan.domain.admin.exception.AdminPendingApprovalException;
import team.startup.gwangsan.domain.admin.exception.AdminUnauthorizedException;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;

@Service
@RequiredArgsConstructor
public class SignInAdminService implements SignInAdminUseCase {

    private final AdminAuthPort adminAuthPort;

    // 검사 순서(역할 → 비밀번호 → 상태)가 응답 계약이다. 비밀번호가 틀리면 상태를 노출하지 않는다.
    @Override
    @Transactional
    public AdminSignInResult execute(AdminSignInCommand command) {
        AdminCredential credential = adminAuthPort.loadCredential(command.nickname());
        if (credential.role() == ActorContext.Role.ROLE_USER) {
            throw new AdminForbiddenException();
        }
        if (!adminAuthPort.passwordMatches(command.password(), credential.encodedPassword())) {
            throw new AdminUnauthorizedException();
        }
        if (credential.status() == AccountStatus.PENDING) {
            throw new AdminPendingApprovalException();
        }
        if (credential.status() != AccountStatus.ACTIVE) {
            throw new AdminForbiddenException();
        }
        IssuedToken token = adminAuthPort.issueToken(credential.phoneNumber(), credential.role());
        return new AdminSignInResult(token.accessToken(), token.refreshToken(),
                token.accessTokenExpiresIn(), token.refreshTokenExpiresIn(), credential.role());
    }
}
