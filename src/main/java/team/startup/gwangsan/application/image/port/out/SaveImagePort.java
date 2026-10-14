package team.startup.gwangsan.application.image.port.out;

import team.startup.gwangsan.domain.image.model.Image;

public interface SaveImagePort {
    Image save(Image image);
}
