package team.startup.gwangsan.adapter.scheduler.suspend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.suspend.port.in.ReleaseExpiredSuspensionsUseCase;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuspendReleaseScheduler {

    private final ReleaseExpiredSuspensionsUseCase releaseExpiredSuspensionsUseCase;

    @Scheduled(cron = "0 0 * * * *")
    public void releaseSuspensions() {
        int released = releaseExpiredSuspensionsUseCase.execute(LocalDateTime.now());
        log.info("Suspension released for {} members", released);
    }
}
