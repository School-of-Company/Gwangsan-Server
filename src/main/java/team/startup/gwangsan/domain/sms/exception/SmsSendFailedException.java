package team.startup.gwangsan.domain.sms.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class SmsSendFailedException extends GlobalException {
    public SmsSendFailedException() {
        super(ErrorCode.AUTH_CODE_GENERATION_FAILURE);
    }
}