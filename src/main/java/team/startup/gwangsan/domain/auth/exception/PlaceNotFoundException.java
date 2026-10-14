package team.startup.gwangsan.domain.auth.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class PlaceNotFoundException extends GlobalException {
    public PlaceNotFoundException() {
        super(ErrorCode.PLACE_NOT_FOUND);
    }
}
