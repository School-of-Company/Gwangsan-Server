package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class ProductNotOngoingException extends GlobalException {
    public ProductNotOngoingException() {
        super(ErrorCode.PRODUCT_NOT_ONGOING);
    }
}
