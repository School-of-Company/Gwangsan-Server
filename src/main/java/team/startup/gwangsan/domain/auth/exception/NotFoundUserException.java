package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundUserException extends GlobalException {
    public NotFoundUserException() {
        super(ErrorCode.NOT_FOUND_USER);
    }
}
