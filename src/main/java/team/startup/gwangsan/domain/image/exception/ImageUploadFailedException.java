package team.startup.gwangsan.domain.image.exception;

import team.startup.gwangsan.global.exception.GlobalException;

public class ImageUploadFailedException extends GlobalException {
    public ImageUploadFailedException() {
        super(Error);
    }
}
