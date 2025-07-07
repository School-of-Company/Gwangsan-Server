package team.startup.gwangsan.domain.notice.exception;

import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.exception.ErrorCode;

public class NoticeForbiddenException extends GlobalException {
    public NoticeForbiddenException() {
        super(ErrorCode.NOTICE_FORBIDDEN);
    }
}
