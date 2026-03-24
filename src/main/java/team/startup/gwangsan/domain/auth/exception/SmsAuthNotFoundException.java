package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class SmsAuthNotFoundException extends GlobalException {
    public SmsAuthNotFoundException() {
        super(ErrorCode.NOT_FOUND_SMS_AUTH);
    }
}
