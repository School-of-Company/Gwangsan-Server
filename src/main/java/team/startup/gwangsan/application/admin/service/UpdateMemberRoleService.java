package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.UpdateMemberRoleCommand;
import team.startup.gwangsan.application.admin.port.in.UpdateMemberRoleUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.domain.admin.policy.AdminPlaceScope;

@Service
@RequiredArgsConstructor
public class UpdateMemberRoleService implements UpdateMemberRoleUseCase {

    private final AdminScopeResolver adminScopeResolver;
    private final AdminMemberPort adminMemberPort;

    @Override
    @Transactional
    public void execute(UpdateMemberRoleCommand command) {
        AdminPlaceScope scope = adminScopeResolver.resolve(command.actor());
        adminMemberPort.requireMember(command.memberId());
        scope.validate(adminMemberPort.loadPlace(command.memberId()).placeId());
        adminMemberPort.changeRole(command.memberId(), command.role(), command.placeId());
    }
}
