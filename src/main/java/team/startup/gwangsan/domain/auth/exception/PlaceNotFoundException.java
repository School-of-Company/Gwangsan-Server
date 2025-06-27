package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class PlaceNotFoundException extends GlobalException {
    public PlaceNotFoundException() {
        super(ErrorCode.PLACE_NOT_FOUND);
    }
}
