package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class RecommenderNotFoundException extends GlobalException {
    public RecommenderNotFoundException() {
        super(ErrorCode.NOT_FOUND_RECOMMENDER);
    }
}
