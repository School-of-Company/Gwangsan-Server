package team.startup.gwangsan.application.image.port.in;

import team.startup.gwangsan.application.image.data.UploadImageCommand;
import team.startup.gwangsan.application.image.data.UploadImageResult;

public interface UploadImageUseCase {
    UploadImageResult execute(UploadImageCommand command);
}
