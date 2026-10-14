package team.startup.gwangsan.domain.block.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class SelfBlockNotAllowedException extends GlobalException {
    public SelfBlockNotAllowedException() {
        super(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
    }
}
