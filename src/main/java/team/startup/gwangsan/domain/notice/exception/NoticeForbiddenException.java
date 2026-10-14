package team.startup.gwangsan.domain.notice.exception;

import team.startup.gwangsan.domain.common.exception.GlobalException;
import team.startup.gwangsan.domain.common.exception.ErrorCode;

public class NoticeForbiddenException extends GlobalException {
    public NoticeForbiddenException() {
        super(ErrorCode.NOTICE_FORBIDDEN);
    }
}
