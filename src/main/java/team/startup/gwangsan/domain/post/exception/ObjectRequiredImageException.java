package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ObjectRequiredImageException extends GlobalException {
    public ObjectRequiredImageException() {
        super(ErrorCode.OBJECT_REQUIRED_IMAGE);
    }
}
