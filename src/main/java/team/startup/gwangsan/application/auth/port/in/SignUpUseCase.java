package team.startup.gwangsan.application.auth.port.in;

import team.startup.gwangsan.application.auth.data.SignUpCommand;

public interface SignUpUseCase {

    void execute(SignUpCommand command);
}
