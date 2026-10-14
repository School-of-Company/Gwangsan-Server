package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class AdminForbiddenException extends GlobalException {
    public AdminForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
