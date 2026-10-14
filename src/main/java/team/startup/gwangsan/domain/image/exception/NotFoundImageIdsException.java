package team.startup.gwangsan.domain.image.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundImageIdsException extends GlobalException {
    public NotFoundImageIdsException() {
        super(ErrorCode.NOT_FOUND_IMAGE_IDS);
    }
}
