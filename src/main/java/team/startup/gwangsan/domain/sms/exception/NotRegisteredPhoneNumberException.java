package team.startup.gwangsan.domain.sms.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotRegisteredPhoneNumberException extends GlobalException {
    public NotRegisteredPhoneNumberException() {
        super(ErrorCode.NOT_REGISTERED_PHONE_NUMBER);
    }
}
