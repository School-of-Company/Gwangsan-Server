package team.startup.gwangsan.domain.review.exception;

import team.startup.gwangsan.domain.common.exception.GlobalException;
import team.startup.gwangsan.domain.common.exception.ErrorCode;

public class AlreadyReviewedException extends GlobalException {
    public AlreadyReviewedException() {
        super(ErrorCode.ALREADY_REVIEWED);
    }
}
