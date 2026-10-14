package team.startup.gwangsan.domain.suspend.model;

import java.time.LocalDateTime;

public record Suspension(
        Long memberId,
        LocalDateTime suspendedAt,
        int suspendedDays,
        LocalDateTime suspendedUntil
) {

    public static Suspension start(Long memberId, int suspendedDays, LocalDateTime now) {
        return new Suspension(memberId, now, suspendedDays, now.plusDays(suspendedDays));
    }
}
