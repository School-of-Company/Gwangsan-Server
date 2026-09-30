package team.startup.gwangsan.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.post.service.impl.ReservationReminderSender;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationReminderScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final ProductReservationRepository reservations;
    private final ReservationReminderSender sender;

    @Scheduled(fixedDelay = 30000)
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now(SEOUL);
        for (Long id : reservations.findDueReminderIds(now, now.plusMinutes(30))) {
            try {
                sender.send(id);
            } catch (Exception e) {
                log.error("Reservation reminder failed. reservationId={}", id, e);
            }
        }
    }
}
