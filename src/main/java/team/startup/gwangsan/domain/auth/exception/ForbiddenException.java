package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ForbiddenException extends GlobalException {
    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
