package team.startup.gwangsan.application.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.auth.port.in.SignOutUseCase;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.RefreshTokenPort;
import team.startup.gwangsan.application.auth.port.out.TokenBlacklistPort;
import team.startup.gwangsan.application.auth.port.out.TokenPort;
import team.startup.gwangsan.domain.auth.exception.NotFoundMemberAccountException;
import team.startup.gwangsan.domain.common.ActorContext;

// 기존과 같이 트랜잭션 없이 Redis 만 변경한다.
@Service
@RequiredArgsConstructor
public class SignOutService implements SignOutUseCase {

    private final AccountPort accountPort;
    private final RefreshTokenPort refreshTokenPort;
    private final TokenPort tokenPort;
    private final TokenBlacklistPort tokenBlacklistPort;

    @Override
    public void execute(ActorContext actor, String accessToken) {
        String phoneNumber = accountPort.findById(actor.memberId())
                .orElseThrow(NotFoundMemberAccountException::new)
                .phoneNumber();
        refreshTokenPort.deleteByPhoneNumber(phoneNumber);
        tokenBlacklistPort.add(accessToken, tokenPort.remainingMillis(accessToken));
    }
}
