package team.startup.gwangsan.adapter.web.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import team.startup.gwangsan.domain.member.model.MemberRole;

public record UpdateMemberRoleRequest(
        @NotNull MemberRole role,
        Integer placeId
) {
}
