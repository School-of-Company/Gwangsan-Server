package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundDummyMemberException extends GlobalException {
    public NotFoundDummyMemberException() {
        super(ErrorCode.NOT_FOUND_DUMMY_MEMBER);
    }
}
