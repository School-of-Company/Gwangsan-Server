package team.startup.gwangsan.domain.suspend.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record SuspendMemberRequest(
        @NotNull Long memberId,
        @NotNull int suspendedDays,
        Long alertId
) {
}
