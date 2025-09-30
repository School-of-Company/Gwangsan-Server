package team.startup.gwangsan.domain.sms.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class AlreadyRegisteredPhoneNumberException extends GlobalException {
    public AlreadyRegisteredPhoneNumberException() {
        super(ErrorCode.ALREADY_REGISTERED_PHONE_NUMBER);
    }
}
