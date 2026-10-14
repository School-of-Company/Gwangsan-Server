package team.startup.gwangsan.adapter.scheduler.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.post.port.in.DueReservationReminderQuery;
import team.startup.gwangsan.application.post.port.in.SendReservationReminderUseCase;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("예약 reminder scheduler")
class ReservationReminderSchedulerTest {

    @Mock DueReservationReminderQuery dueQuery;
    @Mock SendReservationReminderUseCase sendUseCase;
    @InjectMocks ReservationReminderScheduler scheduler;

    @Test
    @DisplayName("it_한 예약의 발송 실패가 다음 예약 발송을 막지 않는다")
    void it_continues_after_one_failure() {
        when(dueQuery.query(any())).thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("Expo rejected")).when(sendUseCase).execute(1L);

        scheduler.sendDueReminders();

        verify(sendUseCase).execute(1L);
        verify(sendUseCase).execute(2L);
    }
}
