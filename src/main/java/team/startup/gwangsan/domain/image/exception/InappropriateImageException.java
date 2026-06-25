package team.startup.gwangsan.domain.image.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class InappropriateImageException extends GlobalException {

    public InappropriateImageException() {
        super(ErrorCode.INAPPROPRIATE_IMAGE);
    }
}
