package team.startup.gwangsan.application.alert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.alert.port.in.AlertMemberCleanupUseCase;
import team.startup.gwangsan.application.alert.port.out.AlertMemberCleanupPort;

@Service
@RequiredArgsConstructor
public class AlertMemberCleanupService implements AlertMemberCleanupUseCase {

    private final AlertMemberCleanupPort alertMemberCleanupPort;

    @Override
    @Transactional
    public void reassignSender(Long memberId, Long dummyMemberId) {
        alertMemberCleanupPort.reassignSender(memberId, dummyMemberId);
    }

    @Override
    @Transactional
    public void deleteReceipts(Long memberId) {
        alertMemberCleanupPort.deleteReceipts(memberId);
    }
}
