package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundProductException extends GlobalException {
    public NotFoundProductException() {
        super(ErrorCode.NOT_FOUND_PRODUCT);
    }
}
