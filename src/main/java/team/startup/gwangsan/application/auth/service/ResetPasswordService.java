package team.startup.gwangsan.application.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.auth.data.ResetPasswordCommand;
import team.startup.gwangsan.application.auth.port.in.ResetPasswordUseCase;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.PasswordHashPort;
import team.startup.gwangsan.application.auth.port.out.PhoneVerificationPort;
import team.startup.gwangsan.domain.auth.exception.NotFoundMemberAccountException;
import team.startup.gwangsan.domain.auth.model.Account;
import team.startup.gwangsan.domain.auth.model.PhoneVerification;

@Service
@RequiredArgsConstructor
public class ResetPasswordService implements ResetPasswordUseCase {

    private final PhoneVerificationPort phoneVerificationPort;
    private final AccountPort accountPort;
    private final PasswordHashPort passwordHashPort;

    @Override
    @Transactional
    public void execute(ResetPasswordCommand command) {
        PhoneVerification.assertCompleted(phoneVerificationPort.findVerified(command.phoneNumber()));
        Account account = accountPort.findByPhoneNumber(command.phoneNumber())
                .orElseThrow(NotFoundMemberAccountException::new);
        accountPort.changePassword(account.id(), passwordHashPort.encode(command.newPassword()));
        phoneVerificationPort.consume(command.phoneNumber());
    }
}
