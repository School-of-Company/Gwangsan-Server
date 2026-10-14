package team.startup.gwangsan.application.alert.port.out;

import java.time.LocalDateTime;

public interface MarkAlertsReadPort {
    void markReadUpTo(Long memberId, Long alertId, LocalDateTime checkedAt);
}
