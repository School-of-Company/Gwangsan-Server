package team.startup.gwangsan.domain.suspend.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundSuspendException extends GlobalException {
    public NotFoundSuspendException() {
        super(ErrorCode.NOT_FOUND_SUSPEND);
    }
}
