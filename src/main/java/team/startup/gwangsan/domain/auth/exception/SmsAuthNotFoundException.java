package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class SmsAuthNotFoundException extends GlobalException {
    public SmsAuthNotFoundException() {
        super(ErrorCode.NOT_FOUND_SMS_AUTH);
    }
}
