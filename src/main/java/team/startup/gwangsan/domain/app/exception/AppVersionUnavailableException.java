package team.startup.gwangsan.domain.app.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class AppVersionUnavailableException extends GlobalException {
    public AppVersionUnavailableException() {
        super(ErrorCode.APP_VERSION_UNAVAILABLE);
    }
}
