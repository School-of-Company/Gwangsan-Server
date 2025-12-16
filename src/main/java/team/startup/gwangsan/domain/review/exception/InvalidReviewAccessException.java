package team.startup.gwangsan.domain.review.exception;

import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.exception.ErrorCode;

public class InvalidReviewAccessException extends GlobalException {
    public InvalidReviewAccessException() {
        super(ErrorCode.INVALID_REVIEW_ACCESS);
    }
}