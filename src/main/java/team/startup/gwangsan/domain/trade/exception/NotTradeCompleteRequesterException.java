package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotTradeCompleteRequesterException extends GlobalException {
    public NotTradeCompleteRequesterException() {
        super(ErrorCode.NOT_TRADE_COMPLETE_REQUESTER);
    }
}
