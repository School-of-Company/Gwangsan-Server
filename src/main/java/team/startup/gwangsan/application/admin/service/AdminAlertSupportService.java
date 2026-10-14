package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.port.in.AdminAlertExistenceQuery;
import team.startup.gwangsan.application.admin.port.in.ReassignAdminAlertMemberUseCase;
import team.startup.gwangsan.application.admin.port.in.RemoveAdminAlertUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminAlertPort;
import team.startup.gwangsan.domain.admin.model.AlertType;

/** 다른 업무에 공개하는 관리자 알림 능력. 모두 호출자 트랜잭션에 참여한다. */
@Service
@RequiredArgsConstructor
public class AdminAlertSupportService implements RemoveAdminAlertUseCase, AdminAlertExistenceQuery,
        ReassignAdminAlertMemberUseCase {

    private final AdminAlertPort adminAlertPort;

    @Override
    @Transactional
    public void removeBySource(AlertType type, Long sourceId) {
        adminAlertPort.findByTypeAndSourceId(type, sourceId)
                .ifPresent(alert -> adminAlertPort.delete(alert.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long alertId) {
        return adminAlertPort.existsById(alertId);
    }

    @Override
    @Transactional
    public void reassign(Long memberId, Long dummyMemberId) {
        adminAlertPort.reassignMember(memberId, dummyMemberId);
    }
}
