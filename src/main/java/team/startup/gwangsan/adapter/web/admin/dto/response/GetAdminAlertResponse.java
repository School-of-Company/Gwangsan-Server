package team.startup.gwangsan.adapter.web.admin.dto.response;

import java.util.List;

public record GetAdminAlertResponse(
        List<GetReportAlertResponse> reports,
        List<GetSignUpAlertResponse> signUps,
        List<GetTradeCancelAlertResponse> tradeCancels
) {
}
