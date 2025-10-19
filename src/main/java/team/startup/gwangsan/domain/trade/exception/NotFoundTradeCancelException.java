package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundTradeCancelException extends GlobalException {
    public NotFoundTradeCancelException() {
        super(ErrorCode.NOT_FOUND_TRADE_CANCEL);
    }
}
