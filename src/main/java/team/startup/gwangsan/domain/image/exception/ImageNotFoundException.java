package team.startup.gwangsan.domain.image.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ImageNotFoundException extends GlobalException {
    public ImageNotFoundException() {
        super(ErrorCode.IMAGE_NOT_FOUND);
    }
}
