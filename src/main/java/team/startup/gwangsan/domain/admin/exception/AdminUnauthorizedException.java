package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class AdminUnauthorizedException extends GlobalException {
    public AdminUnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }
}
