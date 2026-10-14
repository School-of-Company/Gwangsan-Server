package team.startup.gwangsan.domain.notice.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

// 공지 조회자의 회원 상세(소속 지점)가 없다. 기존 NotFoundMemberDetailException 과 같은 오류 코드를 쓴다.
public class NoticeReaderNotFoundException extends GlobalException {
    public NoticeReaderNotFoundException() {
        super(ErrorCode.NOT_FOUND_MEMBER_DETAIL);
    }
}
