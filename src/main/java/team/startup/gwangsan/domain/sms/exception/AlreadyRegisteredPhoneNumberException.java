package team.startup.gwangsan.domain.sms.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class AlreadyRegisteredPhoneNumberException extends GlobalException {
    public AlreadyRegisteredPhoneNumberException() {
        super(ErrorCode.ALREADY_REGISTERED_PHONE_NUMBER);
    }
}
