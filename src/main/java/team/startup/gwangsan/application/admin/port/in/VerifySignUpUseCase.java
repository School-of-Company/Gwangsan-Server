package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.application.admin.data.AdminAlertCommand;

public interface VerifySignUpUseCase {
    void execute(AdminAlertCommand command);
}
