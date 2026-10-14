package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundTradeCancelException extends GlobalException {
    public NotFoundTradeCancelException() {
        super(ErrorCode.NOT_FOUND_TRADE_CANCEL);
    }
}
