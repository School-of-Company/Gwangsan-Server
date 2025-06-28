package team.startup.gwangsan.domain.image.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundImageIdsException extends GlobalException {
    public NotFoundImageIdsException() {
        super(ErrorCode.NOT_FOUND_IMAGE_IDS);
    }
}
