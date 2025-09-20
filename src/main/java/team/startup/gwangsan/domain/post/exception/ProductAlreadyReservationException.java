package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class ProductAlreadyReservationException extends GlobalException {
    public ProductAlreadyReservationException() {
        super(ErrorCode.PRODUCT_ALREADY_RESERVATION);
    }
}
