package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundMemberAccountException extends GlobalException {
    public NotFoundMemberAccountException() {
        super(ErrorCode.NOT_FOUND_MEMBER);
    }
}
