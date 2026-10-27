package team.startup.gwangsan.domain.feedback.exception;

import team.startup.gwangsan.domain.common.exception.GlobalException;
import team.startup.gwangsan.domain.common.exception.ErrorCode;

public class ForbiddenFeedbackException extends GlobalException {
    public ForbiddenFeedbackException() {
        super(ErrorCode.FORBIDDEN_FEEDBACK);
    }
}
