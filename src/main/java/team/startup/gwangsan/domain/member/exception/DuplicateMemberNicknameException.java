package team.startup.gwangsan.domain.member.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class DuplicateMemberNicknameException extends GlobalException {
    public DuplicateMemberNicknameException() {
        super(ErrorCode.DUPLICATE_NICKNAME);
    }
}
