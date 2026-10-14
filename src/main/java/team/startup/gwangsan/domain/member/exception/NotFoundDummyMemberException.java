package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundDummyMemberException extends GlobalException {
    public NotFoundDummyMemberException() {
        super(ErrorCode.NOT_FOUND_DUMMY_MEMBER);
    }
}
