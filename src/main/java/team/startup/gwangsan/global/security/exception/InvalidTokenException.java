package team.startup.gwangsan.global.security.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class InvalidTokenException extends GlobalException {
    public InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN);
    }
}