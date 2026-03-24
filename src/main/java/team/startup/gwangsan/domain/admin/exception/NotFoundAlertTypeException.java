package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundAlertTypeException extends GlobalException {
    public NotFoundAlertTypeException() {
        super(ErrorCode.NOT_FOUND_ALERT_TYPE);
    }
}
