package team.startup.gwangsan.domain.review.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotTradeParticipantException extends GlobalException {
    public NotTradeParticipantException() {
        super(ErrorCode.NOT_TRADE_PARTICIPANT);
    }
}
