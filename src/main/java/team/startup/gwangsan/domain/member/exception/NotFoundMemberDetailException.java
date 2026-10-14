package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundMemberDetailException extends GlobalException {
    public NotFoundMemberDetailException() {
        super(ErrorCode.NOT_FOUND_MEMBER_DETAIL);
    }
}
