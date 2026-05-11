package team.startup.gwangsan.global.event;

import java.time.LocalDateTime;

public record TradeStatusChangedEvent(
        Long roomId,
        Long productId,
        boolean completed,
        LocalDateTime changedAt
) {
}
