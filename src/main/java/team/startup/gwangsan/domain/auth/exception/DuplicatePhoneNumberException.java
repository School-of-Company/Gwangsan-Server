package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class DuplicatePhoneNumberException extends GlobalException {
    public DuplicatePhoneNumberException() {
        super(ErrorCode.DUPLICATE_PHONE_NUMBER);
    }
}
