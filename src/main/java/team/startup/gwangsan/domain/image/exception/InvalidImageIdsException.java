package team.startup.gwangsan.domain.image.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class InvalidImageIdsException extends GlobalException {
    public InvalidImageIdsException() {
        super(ErrorCode.INVALID_IMAGE_IDS);
    }
}
