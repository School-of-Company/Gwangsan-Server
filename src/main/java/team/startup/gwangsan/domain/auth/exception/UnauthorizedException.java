package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class UnauthorizedException extends GlobalException {
    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }
}
