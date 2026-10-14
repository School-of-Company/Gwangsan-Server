package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class InvalidTradeStatisticsPeriodException extends GlobalException {
    public InvalidTradeStatisticsPeriodException() {
        super(ErrorCode.INVALID_TRADE_STATISTICS_PERIOD);
    }
}
