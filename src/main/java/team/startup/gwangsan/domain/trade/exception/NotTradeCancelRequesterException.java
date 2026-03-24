package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotTradeCancelRequesterException extends GlobalException {
    public NotTradeCancelRequesterException() {
        super(ErrorCode.NOT_TRADE_CANCEL_REQUESTER);
    }
}
