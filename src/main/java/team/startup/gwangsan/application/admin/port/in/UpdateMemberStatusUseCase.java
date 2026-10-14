package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.application.admin.data.UpdateMemberStatusCommand;

public interface UpdateMemberStatusUseCase {
    void execute(UpdateMemberStatusCommand command);
}
