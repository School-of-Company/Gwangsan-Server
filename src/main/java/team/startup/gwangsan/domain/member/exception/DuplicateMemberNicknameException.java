package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class DuplicateMemberNicknameException extends GlobalException {
    public DuplicateMemberNicknameException() {
        super(ErrorCode.DUPLICATE_NICKNAME);
    }
}
