package team.startup.gwangsan.domain.block.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundBlockException extends GlobalException {
    public NotFoundBlockException() {
        super(ErrorCode.NOT_FOUND_BLOCK);
    }
}
