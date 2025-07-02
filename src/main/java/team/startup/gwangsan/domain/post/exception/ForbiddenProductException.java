package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ForbiddenProductException extends GlobalException {
    public ForbiddenProductException() {
        super(ErrorCode.FORBIDDEN_PRODUCT);
    }
}
