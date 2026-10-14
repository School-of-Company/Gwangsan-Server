package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class AdminPendingApprovalException extends GlobalException {
    public AdminPendingApprovalException() {
        super(ErrorCode.PENDING_APPROVAL);
    }
}
