package team.startup.gwangsan.adapter.integration.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.port.out.PhoneVerificationPort;
import team.startup.gwangsan.application.sms.port.in.PhoneVerificationQuery;

/** auth → sms 공개 Port 연결. */
@Component
@RequiredArgsConstructor
public class PhoneVerificationAdapter implements PhoneVerificationPort {

    private final PhoneVerificationQuery phoneVerificationQuery;

    @Override
    public boolean isDemoNumber(String phoneNumber) {
        return phoneVerificationQuery.isDemoNumber(phoneNumber);
    }

    @Override
    public Boolean findVerified(String phoneNumber) {
        return phoneVerificationQuery.findVerified(phoneNumber).orElse(null);
    }

    @Override
    public void consume(String phoneNumber) {
        phoneVerificationQuery.consume(phoneNumber);
    }
}
