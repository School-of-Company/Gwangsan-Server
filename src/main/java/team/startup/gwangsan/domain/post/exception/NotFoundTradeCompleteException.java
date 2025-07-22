package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundTradeCompleteException extends GlobalException {
    public NotFoundTradeCompleteException() {
        super(ErrorCode.NOT_FOUND_TRADE_COMPLETE);
    }
}
