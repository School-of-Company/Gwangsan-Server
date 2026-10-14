package team.startup.gwangsan.application.auth.port.in;

import team.startup.gwangsan.application.auth.data.SignInCommand;
import team.startup.gwangsan.application.auth.data.TokenResult;

public interface SignInUseCase {

    TokenResult execute(SignInCommand command);
}
