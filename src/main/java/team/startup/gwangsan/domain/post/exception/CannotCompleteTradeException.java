package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class CannotCompleteTradeException extends GlobalException {
    public CannotCompleteTradeException() {
        super(ErrorCode.CANNOT_COMPLETE_TRADE);
    }
}
