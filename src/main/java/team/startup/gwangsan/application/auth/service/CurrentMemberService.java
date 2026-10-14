package team.startup.gwangsan.application.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.auth.data.CurrentMemberResult;
import team.startup.gwangsan.application.auth.port.in.CurrentMemberQuery;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.domain.auth.exception.NotFoundMemberAccountException;
import team.startup.gwangsan.domain.auth.model.Account;
import team.startup.gwangsan.domain.common.ActorContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentMemberService implements CurrentMemberQuery {

    private final AccountPort accountPort;

    @Override
    @Transactional(readOnly = true)
    public CurrentMemberResult query(ActorContext actor) {
        Account account = accountPort.findById(actor.memberId()).orElseThrow(NotFoundMemberAccountException::new);
        log.info("[TokenAuthenticationService] Current Member: id={}, nickname={}", account.id(), account.nickname());
        return new CurrentMemberResult(account.id(), account.nickname());
    }
}
