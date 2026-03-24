package team.startup.gwangsan.domain.block.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class BlockedMemberException extends GlobalException {
    public BlockedMemberException() {
        super(ErrorCode.BLOCKED_MEMBER);
    }
}
