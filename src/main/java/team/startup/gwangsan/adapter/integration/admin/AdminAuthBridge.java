package team.startup.gwangsan.adapter.integration.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.data.AdminCredential;
import team.startup.gwangsan.application.admin.data.IssuedToken;
import team.startup.gwangsan.application.admin.port.out.AdminAuthPort;
import team.startup.gwangsan.application.auth.port.out.PasswordHashPort;
import team.startup.gwangsan.application.auth.port.out.RefreshTokenPort;
import team.startup.gwangsan.application.auth.port.out.TokenPort;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import java.time.LocalDateTime;

/**
 * 관리자 로그인 자격 조회는 member(PR02) 공개 query, 비밀번호·토큰 발급·refresh 저장은 auth(PR02) 기술 Port 구현을 쓴다.
 * bridge: auth 의 out Port 를 다른 업무 adapter 에서 쓰는 연결이다. 제거: auth 공개 토큰 발급 Port → PR12.
 * 만료 시각은 기존 SignInAdminServiceImpl 과 같이 now + 설정 초로 계산한다.
 */
@Component
@RequiredArgsConstructor
public class AdminAuthBridge implements AdminAuthPort {

    private final MemberAccountQuery memberAccountQuery;
    private final PasswordHashPort passwordHashPort;
    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    public AdminCredential loadCredential(String nickname) {
        MemberAccount account = memberAccountQuery.findByNickname(nickname).orElseThrow(NotFoundMemberException::new);
        return new AdminCredential(account.phoneNumber(), account.passwordHash(),
                ActorContext.Role.valueOf(account.role().name()), AccountStatus.valueOf(account.status().name()));
    }

    @Override
    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordHashPort.matches(rawPassword, encodedPassword);
    }

    @Override
    public IssuedToken issueToken(String phoneNumber, ActorContext.Role role) {
        team.startup.gwangsan.application.auth.data.IssuedToken token = tokenPort.issue(phoneNumber, role.name());
        refreshTokenPort.save(phoneNumber, token.refreshToken());
        return new IssuedToken(token.accessToken(), token.refreshToken(),
                LocalDateTime.now().plusSeconds(token.accessTokenSeconds()),
                LocalDateTime.now().plusSeconds(token.refreshTokenSeconds()));
    }
}
