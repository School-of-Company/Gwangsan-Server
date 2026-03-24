package team.startup.gwangsan.domain.image.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ImageDeleteFailedException extends GlobalException {
    public ImageDeleteFailedException() {
        super(ErrorCode.IMAGE_DELETE_FAILED);
    }
}
