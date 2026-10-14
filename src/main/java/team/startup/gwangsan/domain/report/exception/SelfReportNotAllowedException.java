package team.startup.gwangsan.domain.report.exception;

import team.startup.gwangsan.domain.common.exception.GlobalException;
import team.startup.gwangsan.domain.common.exception.ErrorCode;

public class SelfReportNotAllowedException extends GlobalException {
    public SelfReportNotAllowedException() {
        super(ErrorCode.SELF_REPORT_NOT_ALLOWED);
    }
}
