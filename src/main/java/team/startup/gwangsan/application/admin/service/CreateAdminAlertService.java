package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.CreateAdminAlertCommand;
import team.startup.gwangsan.application.admin.port.in.CreateAdminAlertUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminAlertPort;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.admin.port.out.AdminReportPort;
import team.startup.gwangsan.application.admin.port.out.AdminTradeCancelPort;
import team.startup.gwangsan.domain.admin.exception.NotFoundPendingMemberException;
import team.startup.gwangsan.domain.admin.policy.AdminAlertTitle;

@Service
@RequiredArgsConstructor
public class CreateAdminAlertService implements CreateAdminAlertUseCase {

    private final AdminAlertPort adminAlertPort;
    private final AdminMemberPort adminMemberPort;
    private final AdminReportPort adminReportPort;
    private final AdminTradeCancelPort adminTradeCancelPort;

    @Override
    @Transactional
    public void execute(CreateAdminAlertCommand command) {
        Long sourceId = command.sourceId();
        switch (command.type()) {
            case REPORT -> {
                String title = AdminAlertTitle.report(adminReportPort.categoryOf(sourceId));
                adminMemberPort.requireMember(command.requesterId());
                adminAlertPort.save(command.type(), title, sourceId, command.requesterId());
            }
            case SIGN_UP -> {
                if (!adminMemberPort.isPendingMember(sourceId)) {
                    throw new NotFoundPendingMemberException();
                }
                adminAlertPort.save(command.type(), AdminAlertTitle.SIGN_UP, sourceId, sourceId);
            }
            case TRADE_CANCEL -> adminAlertPort.save(command.type(), AdminAlertTitle.TRADE_CANCEL, sourceId,
                    adminTradeCancelPort.requesterIdOf(sourceId));
        }
    }
}
