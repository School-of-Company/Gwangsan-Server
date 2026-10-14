package team.startup.gwangsan.domain.image.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class ImageUploadFailedException extends GlobalException {
    public ImageUploadFailedException() {
        super(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
}
