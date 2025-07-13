package team.startup.gwangsan.domain.review.exception;

import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.exception.ErrorCode;

public class CannotReviewBeforeTradeException extends GlobalException {
    public CannotReviewBeforeTradeException() {
        super(ErrorCode.CANNOT_REVIEW_BEFORE_TRADE);
    }
}
