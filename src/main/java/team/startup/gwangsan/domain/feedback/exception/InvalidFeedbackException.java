package team.startup.gwangsan.domain.feedback.exception;

import team.startup.gwangsan.domain.common.exception.GlobalException;
import team.startup.gwangsan.domain.common.exception.ErrorCode;

public class InvalidFeedbackException extends GlobalException {
    public InvalidFeedbackException() {
        super(ErrorCode.INVALID_FEEDBACK);
    }
}
