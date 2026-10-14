package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundAlertTypeException extends GlobalException {
    public NotFoundAlertTypeException() {
        super(ErrorCode.NOT_FOUND_ALERT_TYPE);
    }
}
