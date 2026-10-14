package team.startup.gwangsan.adapter.security.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class InvalidTokenException extends GlobalException {
    public InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN);
    }
}