package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.application.admin.data.AdminSignInCommand;
import team.startup.gwangsan.application.admin.data.AdminSignInResult;

public interface SignInAdminUseCase {
    AdminSignInResult execute(AdminSignInCommand command);
}
