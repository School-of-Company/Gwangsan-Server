package team.startup.gwangsan.application.post.data;

import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.List;

public record CreateProductCommand(Long actorId, Type type, Mode mode, String title, String description,
                                   Integer gwangsan, List<Long> imageIds) {
}
