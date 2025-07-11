package team.startup.gwangsan.domain.admin.presentation.dto.response;

import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;

public record SignInAdminResponse(
        TokenResponse token,
        MemberRole role
) {
}
