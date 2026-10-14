package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class AdminForbiddenException extends GlobalException {
    public AdminForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
