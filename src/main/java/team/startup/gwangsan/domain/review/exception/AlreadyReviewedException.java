package team.startup.gwangsan.domain.review.exception;

import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.exception.ErrorCode;

public class AlreadyReviewedException extends GlobalException {
    public AlreadyReviewedException() {
        super(ErrorCode.ALREADY_REVIEWED);
    }
}
