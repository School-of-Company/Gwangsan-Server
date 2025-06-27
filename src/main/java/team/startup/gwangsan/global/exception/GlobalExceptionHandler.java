package team.startup.gwangsan.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handle(GlobalException exception) {
        ErrorResponse response = ErrorResponse.builder()
                .status(exception.getErrorCode().getStatus())
                .message(exception.getErrorCode().getMessage())
                .build();

        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
