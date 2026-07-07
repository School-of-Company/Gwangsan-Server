package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class InappropriateContentException extends GlobalException {

    public InappropriateContentException() {
        super(ErrorCode.INAPPROPRIATE_CONTENT);
    }
}
