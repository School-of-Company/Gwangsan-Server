package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class ReservedProductDeletionException extends GlobalException {
    public ReservedProductDeletionException() {
        super(ErrorCode.RESERVED_PRODUCT_DELETION);
    }
}
