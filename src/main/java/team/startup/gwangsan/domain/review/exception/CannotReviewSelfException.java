package team.startup.gwangsan.domain.review.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class CannotReviewSelfException extends GlobalException {
    public CannotReviewSelfException() {
        super(ErrorCode.CANNOT_REVIEW_SELF);
    }
}
