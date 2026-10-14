package team.startup.gwangsan.adapter.integration.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.sms.port.out.RegisteredPhonePort;

/** sms → member 공개 Port 연결. */
@Component
@RequiredArgsConstructor
public class RegisteredPhoneAdapter implements RegisteredPhonePort {

    private final MemberAccountQuery memberAccountQuery;

    @Override
    public boolean isRegistered(String phoneNumber) {
        return memberAccountQuery.existsByPhoneNumber(phoneNumber);
    }
}
