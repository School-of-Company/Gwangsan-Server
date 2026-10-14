package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class CannotPendingTradeCancelException extends GlobalException {
    public CannotPendingTradeCancelException() {
        super(ErrorCode.CANNOT_PENDING_TRADE_CANCEL);
    }
}
