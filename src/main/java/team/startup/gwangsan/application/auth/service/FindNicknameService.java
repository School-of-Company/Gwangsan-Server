package team.startup.gwangsan.application.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.auth.port.in.FindNicknameQuery;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.PhoneVerificationPort;
import team.startup.gwangsan.domain.auth.exception.NotFoundMemberAccountException;
import team.startup.gwangsan.domain.auth.model.Account;
import team.startup.gwangsan.domain.auth.model.PhoneVerification;

@Service
@RequiredArgsConstructor
public class FindNicknameService implements FindNicknameQuery {

    private final PhoneVerificationPort phoneVerificationPort;
    private final AccountPort accountPort;

    @Override
    @Transactional(readOnly = true)
    public String query(String phoneNumber) {
        PhoneVerification.assertCompleted(phoneVerificationPort.findVerified(phoneNumber));
        Account account = accountPort.findByPhoneNumber(phoneNumber)
                .orElseThrow(NotFoundMemberAccountException::new);
        phoneVerificationPort.consume(phoneNumber);
        return account.nickname();
    }
}
