package team.startup.gwangsan.adapter.web.trade.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TradeCancelRequest(
        @NotEmpty String reason,
        @NotEmpty List<Long> imageIds
) {
}
