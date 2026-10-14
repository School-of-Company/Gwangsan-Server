package team.startup.gwangsan.domain.chat.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

/** 기존 image 의 InvalidImageIdsException 과 같은 오류 코드(INVALID_IMAGE_IDS, 400). */
public class InvalidChatImageIdsException extends GlobalException {
    public InvalidChatImageIdsException() {
        super(ErrorCode.INVALID_IMAGE_IDS);
    }
}
