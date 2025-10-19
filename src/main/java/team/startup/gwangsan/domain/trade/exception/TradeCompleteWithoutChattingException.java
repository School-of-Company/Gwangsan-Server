package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class TradeCompleteWithoutChattingException extends GlobalException {
    public TradeCompleteWithoutChattingException() {
        super(ErrorCode.TRADE_COMPLETE_WITHOUT_CHATTING);
    }
}
