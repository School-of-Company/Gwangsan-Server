package team.startup.gwangsan.domain.block.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class BlockedMemberException extends GlobalException {
    public BlockedMemberException() {
        super(ErrorCode.BLOCKED_MEMBER);
    }
}
