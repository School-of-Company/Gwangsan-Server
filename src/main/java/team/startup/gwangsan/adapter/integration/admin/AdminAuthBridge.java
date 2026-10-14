package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.data.AdminCredential;
import team.startup.gwangsan.application.admin.data.IssuedToken;
import team.startup.gwangsan.application.admin.port.out.AdminAuthPort;
import team.startup.gwangsan.application.auth.port.in.IssueAccountTokenUseCase;
import team.startup.gwangsan.application.auth.port.in.PasswordMatchQuery;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import java.time.LocalDateTime;

/**
 * 관리자 로그인 자격 조회는 member 공개 query, 비밀번호 확인·토큰 발급(refresh 저장 포함)은 auth 공개 Port 를 쓴다.
 * 만료 시각은 기존 SignInAdminServiceImpl 과 같이 now + 설정 초로 계산한다.
 */
@Component
@RequiredArgsConstructor
public class AdminAuthBridge implements AdminAuthPort {

    private final MemberAccountQuery memberAccountQuery;
    private final PasswordMatchQuery passwordMatchQuery;
    private final IssueAccountTokenUseCase issueAccountTokenUseCase;

    @Override
    public AdminCredential loadCredential(String nickname) {
        MemberAccount account = memberAccountQuery.findByNickname(nickname).orElseThrow(NotFoundMemberException::new);
        return new AdminCredential(account.phoneNumber(), account.passwordHash(),
                ActorContext.Role.valueOf(account.role().name()), AccountStatus.valueOf(account.status().name()));
    }

    @Override
    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordMatchQuery.matches(rawPassword, encodedPassword);
    }

    @Override
    public IssuedToken issueToken(String phoneNumber, ActorContext.Role role) {
        team.startup.gwangsan.application.auth.data.IssuedToken token = issueAccountTokenUseCase.execute(phoneNumber, role.name());
        return new IssuedToken(token.accessToken(), token.refreshToken(),
                LocalDateTime.now().plusSeconds(token.accessTokenSeconds()),
                LocalDateTime.now().plusSeconds(token.refreshTokenSeconds()));
    }
}
