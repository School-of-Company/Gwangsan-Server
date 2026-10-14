package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class DummyMemberDeletionNotAllowedException extends GlobalException {
    public DummyMemberDeletionNotAllowedException() {
        super(ErrorCode.DUMMY_MEMBER_DELETION_NOT_ALLOWED);
    }
}
