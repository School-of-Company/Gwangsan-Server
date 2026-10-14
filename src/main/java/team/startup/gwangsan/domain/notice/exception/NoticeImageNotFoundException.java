package team.startup.gwangsan.domain.notice.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

// 공지에 붙일 이미지가 없다. 기존 ImageNotFoundException 과 같은 오류 코드를 쓴다.
public class NoticeImageNotFoundException extends GlobalException {
    public NoticeImageNotFoundException() {
        super(ErrorCode.IMAGE_NOT_FOUND);
    }
}
