package team.startup.gwangsan.application.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.sms.port.in.VerifyCodeUseCase;
import team.startup.gwangsan.application.sms.port.out.SmsCodeStorePort;
import team.startup.gwangsan.domain.sms.exception.NotFoundSmsAuthException;
import team.startup.gwangsan.domain.sms.exception.NotMatchRandomCodeException;
import team.startup.gwangsan.domain.sms.policy.SmsAuthPolicy;
import team.startup.gwangsan.domain.sms.policy.SmsDemoAccount;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCodeService implements VerifyCodeUseCase {

    private final SmsCodeStorePort smsCodeStorePort;

    @Override
    @Transactional
    public void execute(String phoneNumber, String code) {
        if (SmsDemoAccount.isDemo(phoneNumber) && SmsDemoAccount.FIXED_CODE.equals(code)) {
            smsCodeStorePort.saveVerified(phoneNumber, SmsAuthPolicy.VERIFIED_TTL);
            log.info("[SMS] 인증 성공 (데모) - phoneNumber={}", SmsAuthPolicy.mask(phoneNumber));
            return;
        }
        String savedCode = smsCodeStorePort.findCode(phoneNumber).orElseThrow(NotFoundSmsAuthException::new);
        if (!savedCode.equals(code)) {
            throw new NotMatchRandomCodeException();
        }
        smsCodeStorePort.saveVerified(phoneNumber, SmsAuthPolicy.VERIFIED_TTL);
        smsCodeStorePort.deleteCode(phoneNumber);
        log.info("[SMS] 인증 성공 - phoneNumber={}", SmsAuthPolicy.mask(phoneNumber));
    }
}
