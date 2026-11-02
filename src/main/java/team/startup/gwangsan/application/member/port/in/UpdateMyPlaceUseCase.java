package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.UpdateMyPlaceCommand;

public interface UpdateMyPlaceUseCase {

    void execute(UpdateMyPlaceCommand command);
}
