package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class SmsAuthNotCompletedException extends GlobalException {
    public SmsAuthNotCompletedException() {
        super(ErrorCode.SMS_AUTH_NOT_COMPLETED);
    }
}
