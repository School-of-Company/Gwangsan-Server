package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ProductNotReservationException extends GlobalException {
    public ProductNotReservationException() {
        super(ErrorCode.PRODUCT_NOT_RESERVATION);
    }
}
