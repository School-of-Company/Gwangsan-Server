package team.startup.gwangsan.domain.suspend.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundSuspendException extends GlobalException {
    public NotFoundSuspendException() {
        super(ErrorCode.NOT_FOUND_SUSPEND);
    }
}
