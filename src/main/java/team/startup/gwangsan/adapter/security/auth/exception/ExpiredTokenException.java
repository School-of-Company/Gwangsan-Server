package team.startup.gwangsan.adapter.security.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class ExpiredTokenException extends GlobalException {
    public ExpiredTokenException() {
        super(ErrorCode.EXPIRED_TOKEN);
    }
}
