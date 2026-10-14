package team.startup.gwangsan.application.suspend.port.in;

import team.startup.gwangsan.application.suspend.data.SuspendMemberCommand;

public interface SuspendMemberUseCase {
    void execute(SuspendMemberCommand command);
}
