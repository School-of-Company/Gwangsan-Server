package team.startup.gwangsan.application.sms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.sms.port.in.PhoneVerificationQuery;
import team.startup.gwangsan.application.sms.port.out.SmsCodeStorePort;
import team.startup.gwangsan.domain.sms.policy.SmsDemoAccount;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService implements PhoneVerificationQuery {

    private final SmsCodeStorePort smsCodeStorePort;

    @Override
    public boolean isDemoNumber(String phoneNumber) {
        return SmsDemoAccount.isDemo(phoneNumber);
    }

    @Override
    public Optional<Boolean> findVerified(String phoneNumber) {
        return smsCodeStorePort.findVerified(phoneNumber);
    }

    @Override
    public void consume(String phoneNumber) {
        smsCodeStorePort.deleteVerified(phoneNumber);
    }
}
