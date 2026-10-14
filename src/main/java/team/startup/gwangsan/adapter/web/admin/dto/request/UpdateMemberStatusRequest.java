package team.startup.gwangsan.adapter.web.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import team.startup.gwangsan.domain.member.model.MemberStatus;

public record UpdateMemberStatusRequest(
        @NotNull MemberStatus status
) {
}
