package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class SellerNotTradeCompleteException extends GlobalException {
    public SellerNotTradeCompleteException() {
        super(ErrorCode.SELLER_NOT_TRADE_COMPLETED);
    }
}
