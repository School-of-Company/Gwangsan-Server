package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class PendingApprovalException extends GlobalException {
    public PendingApprovalException() {
        super(ErrorCode.PENDING_APPROVAL);
    }
}
