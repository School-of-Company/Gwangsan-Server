package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class CannotSelectSelfException extends GlobalException {
    public CannotSelectSelfException() {
        super(ErrorCode.CANNOT_SELECT_SELF);
    }
}
