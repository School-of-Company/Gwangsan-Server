package team.startup.gwangsan.adapter.web.suspend.dto;

import jakarta.validation.constraints.NotNull;

public record SuspendMemberRequest(
        @NotNull Long memberId,
        @NotNull int suspendedDays,
        Long alertId
) {
}
