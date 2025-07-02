package team.startup.gwangsan.domain.place.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class PlaceMismatchException extends GlobalException {
    public PlaceMismatchException() {
        super(ErrorCode.PLACE_MISMATCH);
    }
}
