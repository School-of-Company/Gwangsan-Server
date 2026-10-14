package team.startup.gwangsan.application.image.port.in;

import team.startup.gwangsan.application.image.data.DeleteImageCommand;

public interface DeleteImageUseCase {
    void execute(DeleteImageCommand command);
}
