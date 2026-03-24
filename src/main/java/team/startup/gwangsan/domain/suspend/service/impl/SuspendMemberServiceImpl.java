package team.startup.gwangsan.domain.suspend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.suspend.entity.Suspend;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;
import team.startup.gwangsan.domain.suspend.service.SuspendMemberService;
import team.startup.gwangsan.global.event.CreateAlertEvent;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SuspendMemberServiceImpl implements SuspendMemberService {

    private final MemberRepository memberRepository;
    private final SuspendRepository suspendRepository;
    private final AdminAlertRepository adminAlertRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void execute(Long memberId, int suspendedDays, Long alertId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusDays(suspendedDays);

        Suspend suspend = Suspend.builder()
                .member(member)
                .suspendedAt(now)
                .suspendedDays(suspendedDays)
                .suspendedUntil(until)
                .build();

        suspend = suspendRepository.save(suspend);

        member.updateMemberStatus(MemberStatus.SUSPENDED);

        if (alertId != null && !adminAlertRepository.existsById(alertId)) {
            applicationEventPublisher.publishEvent(new CreateAlertEvent(
                    alertId,
                    memberId,
                    AlertType.REPORT,
                    suspend.getId()
            ));
        }
    }
}
