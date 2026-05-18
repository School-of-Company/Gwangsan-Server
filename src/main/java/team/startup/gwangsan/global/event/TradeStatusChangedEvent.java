package team.startup.gwangsan.global.event;

import java.time.LocalDateTime;

public record TradeStatusChangedEvent(
        Long roomId,
        Long targetMemberId,
        Long productId,
        boolean completed,
        LocalDateTime changedAt
) {
}
