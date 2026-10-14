package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundMemberAccountException extends GlobalException {
    public NotFoundMemberAccountException() {
        super(ErrorCode.NOT_FOUND_MEMBER);
    }
}
