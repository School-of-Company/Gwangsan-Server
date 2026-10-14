package team.startup.gwangsan.application.image.port.in;

import team.startup.gwangsan.application.image.data.DeleteUnusedImagesCommand;

public interface DeleteUnusedImagesUseCase {
    void execute(DeleteUnusedImagesCommand command);
}
