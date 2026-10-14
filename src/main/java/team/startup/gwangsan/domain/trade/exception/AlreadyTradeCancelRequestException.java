package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class AlreadyTradeCancelRequestException extends GlobalException {
    public AlreadyTradeCancelRequestException() {
        super(ErrorCode.ALREADY_TRADE_CANCEL_REQUEST);
    }
}
