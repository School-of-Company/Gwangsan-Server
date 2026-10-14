package team.startup.gwangsan.domain.sms.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class SmsSendFailedException extends GlobalException {
    public SmsSendFailedException() {
        super(ErrorCode.AUTH_CODE_GENERATION_FAILURE);
    }
}