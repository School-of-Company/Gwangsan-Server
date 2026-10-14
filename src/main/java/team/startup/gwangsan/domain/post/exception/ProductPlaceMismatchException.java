package team.startup.gwangsan.domain.post.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

// place 의 PlaceMismatchException 과 같은 ErrorCode 를 써서 HTTP 응답을 유지한다.
public class ProductPlaceMismatchException extends GlobalException {
    public ProductPlaceMismatchException() {
        super(ErrorCode.PLACE_MISMATCH);
    }
}
