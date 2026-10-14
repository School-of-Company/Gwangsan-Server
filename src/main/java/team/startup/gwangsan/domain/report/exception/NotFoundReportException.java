package team.startup.gwangsan.domain.report.exception;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

public class NotFoundReportException extends GlobalException {
    public NotFoundReportException() {
        super(ErrorCode.NOT_FOUND_REPORT);
    }
}
