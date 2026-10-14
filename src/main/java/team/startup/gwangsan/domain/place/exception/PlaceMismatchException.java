package team.startup.gwangsan.domain.place.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class PlaceMismatchException extends GlobalException {
    public PlaceMismatchException() {
        super(ErrorCode.PLACE_MISMATCH);
    }
}
