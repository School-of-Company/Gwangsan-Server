package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class TradeAlreadyCompleteException extends GlobalException {
    public TradeAlreadyCompleteException() {
        super(ErrorCode.TRADE_ALREADY_COMPLETE);
    }
}
