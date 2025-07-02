package team.startup.gwangsan.domain.post.service;

import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;

import java.util.List;

public interface CreateProductService {
    void execute(Type type, Mode mode, String title, String description, Integer gwangsan, List<Long> imageIds);
}
