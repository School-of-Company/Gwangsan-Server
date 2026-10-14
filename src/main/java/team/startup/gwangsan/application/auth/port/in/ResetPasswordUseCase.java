package team.startup.gwangsan.application.auth.port.in;

import team.startup.gwangsan.application.auth.data.ResetPasswordCommand;

public interface ResetPasswordUseCase {

    void execute(ResetPasswordCommand command);
}
