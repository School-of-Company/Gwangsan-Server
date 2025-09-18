package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class TradeAlreadyCompleteRequestException extends GlobalException {
    public TradeAlreadyCompleteRequestException() {
        super(ErrorCode.TRADE_ALREADY_COMPLETE_REQUEST);
    }
}
