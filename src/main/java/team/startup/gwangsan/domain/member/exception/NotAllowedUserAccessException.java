package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotAllowedUserAccessException extends GlobalException {
    public NotAllowedUserAccessException() {
        super(ErrorCode.NOT_ALLOWED_USER_ACCESS);
    }
}
