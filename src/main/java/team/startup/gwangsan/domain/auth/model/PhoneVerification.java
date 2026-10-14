package team.startup.gwangsan.domain.auth.model;

import team.startup.gwangsan.domain.auth.exception.SmsAuthNotCompletedException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;

/** SMS 인증 완료 표시. 값이 없으면 인증 요청 기록이 없는 것이다. */
public final class PhoneVerification {

    private PhoneVerification() {
    }

    public static void assertCompleted(Boolean verified) {
        if (verified == null) {
            throw new SmsAuthNotFoundException();
        }
        if (!Boolean.TRUE.equals(verified)) {
            throw new SmsAuthNotCompletedException();
        }
    }
}
