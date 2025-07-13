package team.startup.gwangsan.domain.report.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class InvalidReportTypeException extends GlobalException {
    public InvalidReportTypeException() {
        super(ErrorCode.INVALID_REPORT_TYPE);
    }
}