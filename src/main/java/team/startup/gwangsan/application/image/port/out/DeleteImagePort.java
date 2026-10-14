package team.startup.gwangsan.application.image.port.out;

import java.util.List;

public interface DeleteImagePort {
    void deleteById(Long imageId);

    void deleteAllByIdInBatch(List<Long> imageIds);
}
