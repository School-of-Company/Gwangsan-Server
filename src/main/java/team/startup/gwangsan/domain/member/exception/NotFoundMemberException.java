package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundMemberException extends GlobalException {
    public NotFoundMemberException() {
        super(ErrorCode.NOT_FOUND_MEMBER);
    }
}

