package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotAllowedUserAccessException extends GlobalException {
    public NotAllowedUserAccessException() {
        super(ErrorCode.NOT_ALLOWED_USER_ACCESS);
    }
}
