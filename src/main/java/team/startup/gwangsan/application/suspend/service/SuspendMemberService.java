package team.startup.gwangsan.application.suspend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.suspend.data.SuspendMemberCommand;
import team.startup.gwangsan.application.suspend.port.in.SuspendMemberUseCase;
import team.startup.gwangsan.application.suspend.port.out.ChangeMemberStatusPort;
import team.startup.gwangsan.application.suspend.port.out.LoadAdminAlertPort;
import team.startup.gwangsan.application.suspend.port.out.RequestSuspensionAlertPort;
import team.startup.gwangsan.application.suspend.port.out.SaveSuspensionPort;
import team.startup.gwangsan.domain.suspend.model.Suspension;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SuspendMemberService implements SuspendMemberUseCase {

    private final ChangeMemberStatusPort changeMemberStatusPort;
    private final SaveSuspensionPort saveSuspensionPort;
    private final LoadAdminAlertPort loadAdminAlertPort;
    private final RequestSuspensionAlertPort requestSuspensionAlertPort;

    @Override
    @Transactional
    public void execute(SuspendMemberCommand command) {
        changeMemberStatusPort.checkMemberExists(command.memberId());

        Suspension suspension = Suspension.start(command.memberId(), command.suspendedDays(), LocalDateTime.now());
        Long suspendId = saveSuspensionPort.save(suspension);
        changeMemberStatusPort.suspend(command.memberId());

        if (command.alertId() != null && !loadAdminAlertPort.exists(command.alertId())) {
            requestSuspensionAlertPort.requestReportAlert(command.alertId(), command.memberId(), suspendId);
        }
    }
}
