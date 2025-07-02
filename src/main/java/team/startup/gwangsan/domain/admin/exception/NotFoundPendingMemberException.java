package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundPendingMemberException extends GlobalException {
    public NotFoundPendingMemberException() {
        super(ErrorCode.NOT_FOUND_PENDING_MEMBER);
    }
}
