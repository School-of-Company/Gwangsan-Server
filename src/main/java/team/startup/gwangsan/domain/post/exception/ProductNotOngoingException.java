package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ProductNotOngoingException extends GlobalException {
    public ProductNotOngoingException() {
        super(ErrorCode.PRODUCT_NOT_ONGOING);
    }
}
