package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class DongNotFoundException extends GlobalException {
    public DongNotFoundException() {
        super(ErrorCode.DONG_NOT_FOUND);
    }
}