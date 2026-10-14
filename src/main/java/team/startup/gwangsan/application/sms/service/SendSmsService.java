package team.startup.gwangsan.application.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.sms.port.in.SendSmsUseCase;
import team.startup.gwangsan.application.sms.port.out.RegisteredPhonePort;
import team.startup.gwangsan.application.sms.port.out.SmsCodeStorePort;
import team.startup.gwangsan.application.sms.port.out.SmsSenderPort;
import team.startup.gwangsan.domain.sms.exception.AlreadyRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.policy.SmsAuthPolicy;
import team.startup.gwangsan.domain.sms.policy.SmsDemoAccount;

/** 가입 인증번호. 코드 저장 후 비동기 발송한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendSmsService implements SendSmsUseCase {

    private final RegisteredPhonePort registeredPhonePort;
    private final SmsCodeStorePort smsCodeStorePort;
    private final SmsSenderPort smsSenderPort;

    @Override
    public void execute(String phoneNumber) {
        if (SmsDemoAccount.isDemo(phoneNumber)) {
            log.info("[SMS] 데모 번호 요청 - 발송 생략");
            return;
        }
        if (registeredPhonePort.isRegistered(phoneNumber)) {
            throw new AlreadyRegisteredPhoneNumberException();
        }
        String code = SmsAuthPolicy.generateCode();
        IssueCode.save(smsCodeStorePort, phoneNumber, code);
        log.info("[SMS] 인증 정보 저장 성공 - phoneNumber={}", SmsAuthPolicy.mask(phoneNumber));
        smsSenderPort.sendAsync(phoneNumber, "[시민화폐광산] 인증번호는 " + code + "입니다. 3분 이내에 입력해주세요.");
    }
}
