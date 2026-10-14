package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.application.admin.data.CreateAdminAlertCommand;

public interface CreateAdminAlertUseCase {
    void execute(CreateAdminAlertCommand command);
}
