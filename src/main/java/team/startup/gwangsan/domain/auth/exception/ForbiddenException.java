package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class ForbiddenException extends GlobalException {
    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
