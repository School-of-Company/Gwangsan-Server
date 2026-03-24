package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class AlreadyTradeCancelRequestException extends GlobalException {
    public AlreadyTradeCancelRequestException() {
        super(ErrorCode.ALREADY_TRADE_CANCEL_REQUEST);
    }
}
