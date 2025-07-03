package team.startup.gwangsan.domain.admin.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;

public record UpdateMemberRoleRequest(
        @NotNull MemberRole role
) {
}
