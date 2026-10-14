package team.startup.gwangsan.domain.alert.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class UnsupportedAlertTypeException extends GlobalException {
    public UnsupportedAlertTypeException() {
        super(ErrorCode.NOT_FOUND_ALERT_TYPE);
    }
}
