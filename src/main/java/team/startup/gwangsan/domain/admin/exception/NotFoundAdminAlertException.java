package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundAdminAlertException extends GlobalException {
    public NotFoundAdminAlertException() {
        super(ErrorCode.NOT_FOUND_ADMIN_ALERT);
    }
}
