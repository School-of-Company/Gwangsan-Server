package team.startup.gwangsan.domain.review.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class CannotReviewSelfException extends GlobalException {
    public CannotReviewSelfException() {
        super(ErrorCode.CANNOT_REVIEW_SELF);
    }
}
