package team.startup.gwangsan.adapter.scheduler.post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.port.in.DueReservationReminderQuery;
import team.startup.gwangsan.application.post.port.in.SendReservationReminderUseCase;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationReminderScheduler {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final DueReservationReminderQuery dueReservationReminderQuery;
    private final SendReservationReminderUseCase sendReservationReminderUseCase;

    // 예약마다 별도 트랜잭션이다. 한 건의 실패(발송 실패 포함)가 다른 예약 발송을 막지 않는다.
    @Scheduled(fixedDelay = 30000)
    public void sendDueReminders() {
        for (Long id : dueReservationReminderQuery.query(LocalDateTime.now(SEOUL))) {
            try {
                sendReservationReminderUseCase.execute(id);
            } catch (Exception e) {
                log.error("Reservation reminder failed. reservationId={}", id, e);
            }
        }
    }
}
