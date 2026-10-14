package team.startup.gwangsan.domain.admin.presentation.dto.response;

import team.startup.gwangsan.adapter.web.auth.dto.response.TokenResponse;
import team.startup.gwangsan.domain.member.model.MemberRole;

public record SignInAdminResponse(
        TokenResponse token,
        MemberRole role
) {
}
