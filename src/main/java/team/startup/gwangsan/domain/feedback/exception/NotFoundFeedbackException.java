package team.startup.gwangsan.domain.feedback.exception;

import team.startup.gwangsan.domain.common.exception.GlobalException;
import team.startup.gwangsan.domain.common.exception.ErrorCode;

public class NotFoundFeedbackException extends GlobalException {
    public NotFoundFeedbackException() {
        super(ErrorCode.NOT_FOUND_FEEDBACK);
    }
}
