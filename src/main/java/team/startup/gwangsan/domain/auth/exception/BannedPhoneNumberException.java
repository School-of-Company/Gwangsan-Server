package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class BannedPhoneNumberException extends GlobalException {
    public BannedPhoneNumberException() {
        super(ErrorCode.BANNED_PHONE_NUMBER);
    }
}
