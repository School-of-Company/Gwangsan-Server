package team.startup.gwangsan.domain.block.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class AlreadyBlockedException extends GlobalException {
    public AlreadyBlockedException() {
        super(ErrorCode.ALREADY_BLOCKED);
    }
}
