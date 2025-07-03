package team.startup.gwangsan.domain.admin.presentation.dto.response;

import java.util.List;

public record GetAdminAlertResponse(
        List<GetReportAlertResponse> reports,
        List<GetSignUpAlertResponse> signUps
) {
}
