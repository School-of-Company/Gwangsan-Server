package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class AdminUnauthorizedException extends GlobalException {
    public AdminUnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }
}
