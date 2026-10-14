package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.AdminAlertCommand;
import team.startup.gwangsan.application.admin.data.MemberAlertKind;
import team.startup.gwangsan.application.admin.port.in.RejectAdminAlertUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminAlertPort;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.admin.port.out.AdminNotificationPort;
import team.startup.gwangsan.application.admin.port.out.AdminReportPort;
import team.startup.gwangsan.application.admin.port.out.AdminTradeCancelPort;
import team.startup.gwangsan.domain.admin.model.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.model.AdminAlert;
import team.startup.gwangsan.domain.admin.policy.AdminPlaceScope;

@Service
@RequiredArgsConstructor
public class RejectAdminAlertService implements RejectAdminAlertUseCase {

    private final AdminScopeResolver adminScopeResolver;
    private final AdminAlertPort adminAlertPort;
    private final AdminMemberPort adminMemberPort;
    private final AdminTradeCancelPort adminTradeCancelPort;
    private final AdminReportPort adminReportPort;
    private final AdminNotificationPort adminNotificationPort;

    /** 잠금과 격리 수준의 이유는 {@code TradeCancelServiceImpl} 주석 참고. */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(AdminAlertCommand command) {
        AdminPlaceScope scope = adminScopeResolver.resolve(command.actor());
        AdminAlert alert = adminAlertPort.findById(command.alertId())
                .orElseThrow(NotFoundAdminAlertException::new);
        scope.validate(adminMemberPort.loadPlace(alert.requesterId()).placeId());
        // 기각도 철회 요청·승인·자동 철회와 같은 잠금을 거쳐야 한다. 그러지 않으면
        // 이미 승인된(APPROVED) 철회를 REJECTED 로 덮어써 광산과 상태가 어긋난다.
        if (alert.type() == AlertType.TRADE_CANCEL) {
            adminTradeCancelPort.lockProduct(adminTradeCancelPort.productIdOf(alert.sourceId()));
        }
        adminAlertPort.delete(alert.id());
        switch (alert.type()) {
            case SIGN_UP -> adminMemberPort.rejectSignUp(alert.requesterId());
            case REPORT -> adminNotificationPort.notifyMember(alert.sourceId(),
                    adminReportPort.reporterIdOf(alert.sourceId()), MemberAlertKind.REPORT_REJECT);
            case TRADE_CANCEL -> adminNotificationPort.notifyMember(alert.sourceId(),
                    adminTradeCancelPort.rejectPending(alert.sourceId()), MemberAlertKind.TRADE_CANCEL_REJECT);
        }
    }
}
