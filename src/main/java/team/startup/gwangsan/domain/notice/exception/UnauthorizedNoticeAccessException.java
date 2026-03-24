package team.startup.gwangsan.domain.notice.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class UnauthorizedNoticeAccessException extends GlobalException {
    public UnauthorizedNoticeAccessException() {
        super(ErrorCode.UNAUTHORIZED_NOTICE_ACCESS);
    }
}
