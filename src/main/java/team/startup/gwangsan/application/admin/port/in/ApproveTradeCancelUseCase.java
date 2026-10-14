package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.application.admin.data.AdminAlertCommand;

public interface ApproveTradeCancelUseCase {
    void execute(AdminAlertCommand command);
}
