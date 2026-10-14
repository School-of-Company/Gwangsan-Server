package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.UpdateMyInfoCommand;

public interface UpdateMyInfoUseCase {

    void execute(UpdateMyInfoCommand command);
}
