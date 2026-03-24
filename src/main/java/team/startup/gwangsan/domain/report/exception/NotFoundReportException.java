package team.startup.gwangsan.domain.report.exception;

import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

public class NotFoundReportException extends GlobalException {
    public NotFoundReportException() {
        super(ErrorCode.NOT_FOUND_REPORT);
    }
}
