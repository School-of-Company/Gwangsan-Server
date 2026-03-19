package team.startup.gwangsan.domain.block.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class SelfBlockNotAllowedException extends GlobalException {
    public SelfBlockNotAllowedException() {
        super(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
    }
}
