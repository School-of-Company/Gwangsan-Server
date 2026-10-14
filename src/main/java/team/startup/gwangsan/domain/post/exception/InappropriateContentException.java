package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class InappropriateContentException extends GlobalException {

    public InappropriateContentException() {
        super(ErrorCode.INAPPROPRIATE_CONTENT);
    }
}
