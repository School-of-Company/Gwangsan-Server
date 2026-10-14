package team.startup.gwangsan.domain.report.exception;

import team.startup.gwangsan.domain.common.exception.GlobalException;
import team.startup.gwangsan.domain.common.exception.ErrorCode;

public class AlreadyReportedException extends GlobalException {
    public AlreadyReportedException() {
        super(ErrorCode.ALREADY_REPORTED);
    }
}
