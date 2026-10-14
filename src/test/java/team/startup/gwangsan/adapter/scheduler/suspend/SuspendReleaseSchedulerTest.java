package team.startup.gwangsan.adapter.scheduler.suspend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import team.startup.gwangsan.application.suspend.port.in.ReleaseExpiredSuspensionsUseCase;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SuspendReleaseScheduler 단위 테스트")
class SuspendReleaseSchedulerTest {

    @Test
    @DisplayName("it_현재 시각으로 해제 유스케이스를 실행한다")
    void it_runs_release_use_case_with_now() {
        ReleaseExpiredSuspensionsUseCase useCase = mock(ReleaseExpiredSuspensionsUseCase.class);
        LocalDateTime before = LocalDateTime.now();

        new SuspendReleaseScheduler(useCase).releaseSuspensions();

        verify(useCase).execute(argThat(now -> !now.isBefore(before) && !now.isAfter(LocalDateTime.now())));
    }

    @Test
    @DisplayName("it_매시 정각 cron 을 유지한다")
    void it_keeps_hourly_cron() throws NoSuchMethodException {
        Scheduled scheduled = SuspendReleaseScheduler.class.getMethod("releaseSuspensions").getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("0 0 * * * *");
    }
}
