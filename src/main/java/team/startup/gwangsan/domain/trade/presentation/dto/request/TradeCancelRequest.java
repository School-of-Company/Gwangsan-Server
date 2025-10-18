package team.startup.gwangsan.domain.trade.presentation.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TradeCancelRequest(
        @NotEmpty String reason,
        @NotEmpty List<Long> imageIds
) {
}
