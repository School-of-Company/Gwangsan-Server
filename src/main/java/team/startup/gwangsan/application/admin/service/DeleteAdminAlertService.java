package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.admin.data.AdminAlertCommand;
import team.startup.gwangsan.application.admin.port.in.DeleteAdminAlertUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminAlertPort;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.model.AdminAlert;
import team.startup.gwangsan.domain.admin.policy.AdminPlaceScope;

@Service
@RequiredArgsConstructor
public class DeleteAdminAlertService implements DeleteAdminAlertUseCase {

    private final AdminScopeResolver adminScopeResolver;
    private final AdminAlertPort adminAlertPort;
    private final AdminMemberPort adminMemberPort;

    // 기준선 그대로 트랜잭션을 열지 않는다(각 저장소 호출의 기본 트랜잭션).
    @Override
    public void execute(AdminAlertCommand command) {
        AdminPlaceScope scope = adminScopeResolver.resolve(command.actor());
        AdminAlert alert = adminAlertPort.findById(command.alertId())
                .orElseThrow(NotFoundAdminAlertException::new);
        scope.validate(adminMemberPort.loadPlace(alert.requesterId()).placeId());
        adminAlertPort.delete(alert.id());
    }
}
