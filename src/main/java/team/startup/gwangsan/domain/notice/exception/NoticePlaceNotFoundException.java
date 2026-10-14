package team.startup.gwangsan.domain.notice.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

// 공지 대상 지점이 없다. 기존 PlaceNotFoundException 과 같은 오류 코드를 쓴다.
public class NoticePlaceNotFoundException extends GlobalException {
    public NoticePlaceNotFoundException() {
        super(ErrorCode.PLACE_NOT_FOUND);
    }
}
