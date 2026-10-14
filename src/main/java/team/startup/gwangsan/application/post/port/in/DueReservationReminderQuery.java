package team.startup.gwangsan.application.post.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface DueReservationReminderQuery {
    List<Long> query(LocalDateTime now);
}
