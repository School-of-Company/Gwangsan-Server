package team.startup.gwangsan.domain.report.exception;

import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.exception.ErrorCode;

public class AlreadyReportedException extends GlobalException {
    public AlreadyReportedException() {
        super(ErrorCode.ALREADY_REPORTED);
    }
}
