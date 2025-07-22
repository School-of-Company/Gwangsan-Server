package team.startup.gwangsan.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.suspend.entity.Suspend;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuspendReleaseScheduler {

    private final SuspendRepository suspendRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void releaseSuspensions() {
        List<Suspend> suspends = suspendRepository.findAllBySuspendedUntilBefore(LocalDateTime.now());

        for (Suspend suspend : suspends) {
            Member member = suspend.getMember();
            member.updateMemberStatus(MemberStatus.ACTIVE);
        }

        log.info("Suspension released for {} members", suspends.size());
    }
}
