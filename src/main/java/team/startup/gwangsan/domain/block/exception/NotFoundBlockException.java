package team.startup.gwangsan.domain.block.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundBlockException extends GlobalException {
    public NotFoundBlockException() {
        super(ErrorCode.NOT_FOUND_BLOCK);
    }
}
