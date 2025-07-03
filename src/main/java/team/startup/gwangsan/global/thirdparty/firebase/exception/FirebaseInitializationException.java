package team.startup.gwangsan.global.thirdparty.firebase.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class FirebaseInitializationException extends GlobalException {
    public FirebaseInitializationException() {
        super(ErrorCode.FAILED_TO_INITIALIZE_FIREBASE);
    }
}
