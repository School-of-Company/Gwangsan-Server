package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ReservedProductDeletionException extends GlobalException {
    public ReservedProductDeletionException() {
        super(ErrorCode.RESERVED_PRODUCT_DELETION);
    }
}
