package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class BannedPhoneNumberException extends GlobalException {
    public BannedPhoneNumberException() {
        super(ErrorCode.BANNED_PHONE_NUMBER);
    }
}
