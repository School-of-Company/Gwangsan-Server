package team.startup.gwangsan.domain.admin.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdjustGwangsanRequest(
        @NotNull Integer gwangsan
) {
}
