package team.startup.gwangsan.adapter.security.auth.exception;

import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.exception.ErrorCode;

public class InvalidMemberPrincipalException extends GlobalException {
    public InvalidMemberPrincipalException() {
        super(ErrorCode.INVALID_MEMBER_PRINCIPAL);
    }
}
