package team.startup.gwangsan.domain.trade.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class TradeParticipantOnlyException extends GlobalException {
    public TradeParticipantOnlyException() {
        super(ErrorCode.TRADE_PARTICIPANT_ONLY);
    }
}
