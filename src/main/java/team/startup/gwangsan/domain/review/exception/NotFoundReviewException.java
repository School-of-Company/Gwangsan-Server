package team.startup.gwangsan.domain.review.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundReviewException extends GlobalException {
    public NotFoundReviewException() {
        super(ErrorCode.NOT_FOUND_REVIEW);
    }
}
