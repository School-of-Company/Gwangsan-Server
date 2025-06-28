package team.startup.gwangsan.domain.sms.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotMatchRandomCodeException extends GlobalException {
    public NotMatchRandomCodeException() {
        super(ErrorCode.NOT_MATCH_RANDOM_CODE);
    }
}
