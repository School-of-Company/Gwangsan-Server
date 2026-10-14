package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundPendingMemberException extends GlobalException {
    public NotFoundPendingMemberException() {
        super(ErrorCode.NOT_FOUND_PENDING_MEMBER);
    }
}
