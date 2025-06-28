package team.startup.gwangsan.domain.sms.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class TooManyRequestAuthCodeException extends GlobalException {
    public TooManyRequestAuthCodeException() {
        super(ErrorCode.TOO_MANY_REQUEST_AUTH_CODE);
    }
}
