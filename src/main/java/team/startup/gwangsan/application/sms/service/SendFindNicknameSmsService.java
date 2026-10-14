package team.startup.gwangsan.application.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.sms.port.in.SendFindNicknameSmsUseCase;
import team.startup.gwangsan.application.sms.port.out.RegisteredPhonePort;
import team.startup.gwangsan.application.sms.port.out.SmsCodeStorePort;
import team.startup.gwangsan.application.sms.port.out.SmsSenderPort;
import team.startup.gwangsan.domain.sms.exception.NotRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.policy.SmsAuthPolicy;
import team.startup.gwangsan.domain.sms.policy.SmsDemoAccount;

/** 별칭 찾기만 동기 발송한다. 발송이 성공해야 코드를 저장한다(기존 동작). */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendFindNicknameSmsService implements SendFindNicknameSmsUseCase {

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
        IssueCode.countAttempt(smsCodeStorePort, phoneNumber);
        smsSenderPort.send(phoneNumber, "[시민화폐광산] 별칭 찾기 인증번호는 " + code + "입니다. 3분 이내에 입력해주세요.");
        smsCodeStorePort.saveCode(phoneNumber, code, SmsAuthPolicy.CODE_TTL);
    }
}
