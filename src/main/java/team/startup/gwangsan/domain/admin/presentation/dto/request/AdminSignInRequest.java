package team.startup.gwangsan.domain.admin.presentation.dto.request;

import jakarta.validation.constraints.NotEmpty;

public record AdminSignInRequest(
        @NotEmpty String nickname,
        @NotEmpty String password
) {
}
