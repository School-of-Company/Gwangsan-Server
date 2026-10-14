package team.startup.gwangsan.domain.alert.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class UnsupportedAlertTypeException extends GlobalException {
    public UnsupportedAlertTypeException() {
        super(ErrorCode.NOT_FOUND_ALERT_TYPE);
    }
}
