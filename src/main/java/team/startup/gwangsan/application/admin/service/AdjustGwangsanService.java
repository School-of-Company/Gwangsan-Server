package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.AdjustGwangsanCommand;
import team.startup.gwangsan.application.admin.port.in.AdjustGwangsanUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.domain.admin.policy.AdminPlaceScope;

@Service
@RequiredArgsConstructor
public class AdjustGwangsanService implements AdjustGwangsanUseCase {

    private final AdminScopeResolver adminScopeResolver;
    private final AdminMemberPort adminMemberPort;

    @Override
    @Transactional
    public void execute(AdjustGwangsanCommand command) {
        AdminPlaceScope scope = adminScopeResolver.resolve(command.actor());
        Integer targetPlaceId = adminMemberPort.loadAdjustTargetPlaceId(command.memberId());
        scope.validate(targetPlaceId);
        adminMemberPort.adjustGwangsan(command.memberId(), command.gwangsan());
    }
}
