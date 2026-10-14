package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.UpdateMemberStatusCommand;
import team.startup.gwangsan.application.admin.port.in.UpdateMemberStatusUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.domain.admin.model.AccountStatus;
import team.startup.gwangsan.domain.admin.policy.AdminPlaceScope;

@Service
@RequiredArgsConstructor
public class UpdateMemberStatusService implements UpdateMemberStatusUseCase {

    private final AdminScopeResolver adminScopeResolver;
    private final AdminMemberPort adminMemberPort;

    @Override
    @Transactional
    public void execute(UpdateMemberStatusCommand command) {
        AdminPlaceScope scope = adminScopeResolver.resolve(command.actor());
        adminMemberPort.requireMember(command.memberId());
        scope.validate(adminMemberPort.loadPlace(command.memberId()).placeId());
        if (command.status() == AccountStatus.WITHDRAWN) {
            adminMemberPort.withdrawBanned(command.memberId());
            return;
        }
        adminMemberPort.changeStatus(command.memberId(), command.status());
    }
}
