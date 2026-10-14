package team.startup.gwangsan.application.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.sms.port.in.SendResetPasswordSmsUseCase;
import team.startup.gwangsan.application.sms.port.out.RegisteredPhonePort;
import team.startup.gwangsan.application.sms.port.out.SmsCodeStorePort;
import team.startup.gwangsan.application.sms.port.out.SmsSenderPort;
import team.startup.gwangsan.domain.sms.exception.NotRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.policy.SmsAuthPolicy;
import team.startup.gwangsan.domain.sms.policy.SmsDemoAccount;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendResetPasswordSmsService implements SendResetPasswordSmsUseCase {

    private final RegisteredPhonePort registeredPhonePort;
    private final SmsCodeStorePort smsCodeStorePort;
    private final SmsSenderPort smsSenderPort;

    @Override
    @Transactional
    public void execute(String phoneNumber) {
        if (SmsDemoAccount.isDemo(phoneNumber)) {
            log.info("[SMS] 데모 번호 요청 - 발송 생략");
            return;
        }
        if (!registeredPhonePort.isRegistered(phoneNumber)) {
            throw new NotRegisteredPhoneNumberException();
        }
        String code = SmsAuthPolicy.generateCode();
        IssueCode.save(smsCodeStorePort, phoneNumber, code);
        smsSenderPort.sendAsync(phoneNumber,
                "[시민화폐광산] 비밀번호 재설정 인증번호는 " + code + "입니다. 3분 이내에 입력해주세요.");
    }
}
