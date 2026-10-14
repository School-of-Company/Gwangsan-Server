package team.startup.gwangsan.adapter.web.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdjustGwangsanRequest(
        @NotNull Integer gwangsan
) {
}
