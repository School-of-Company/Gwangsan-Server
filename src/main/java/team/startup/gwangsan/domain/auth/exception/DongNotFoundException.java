package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class DongNotFoundException extends GlobalException {
    public DongNotFoundException() {
        super(ErrorCode.DONG_NOT_FOUND);
    }
}