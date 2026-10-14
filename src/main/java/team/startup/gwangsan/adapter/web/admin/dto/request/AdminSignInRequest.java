package team.startup.gwangsan.adapter.web.admin.dto.request;

import jakarta.validation.constraints.NotEmpty;

public record AdminSignInRequest(
        @NotEmpty String nickname,
        @NotEmpty String password
) {
}
