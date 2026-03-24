package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class DuplicateNicknameException extends GlobalException {
    public DuplicateNicknameException() {
        super(ErrorCode.DUPLICATE_NICKNAME);
    }
}
