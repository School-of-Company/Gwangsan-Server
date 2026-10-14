package team.startup.gwangsan.domain.admin.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class AdminPlaceMismatchException extends GlobalException {
    public AdminPlaceMismatchException() {
        super(ErrorCode.PLACE_MISMATCH);
    }
}
