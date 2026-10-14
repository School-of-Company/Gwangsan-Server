package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundTradeCompleteException extends GlobalException {
    public NotFoundTradeCompleteException() {
        super(ErrorCode.NOT_FOUND_TRADE_COMPLETE);
    }
}
