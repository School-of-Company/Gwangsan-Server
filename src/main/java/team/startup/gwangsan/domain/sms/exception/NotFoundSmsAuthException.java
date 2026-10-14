package team.startup.gwangsan.domain.sms.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundSmsAuthException extends GlobalException {
    public NotFoundSmsAuthException() {
        super(ErrorCode.NOT_FOUND_SMS_AUTH);
    }
}
