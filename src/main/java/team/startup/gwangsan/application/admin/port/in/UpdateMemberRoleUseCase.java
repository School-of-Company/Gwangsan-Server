package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.application.admin.data.UpdateMemberRoleCommand;

public interface UpdateMemberRoleUseCase {
    void execute(UpdateMemberRoleCommand command);
}
