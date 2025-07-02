package team.startup.gwangsan.domain.post.service;

import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;

import java.util.List;

public interface UpdateProductService {
    void execute(Long productId, Type type, Mode mode, String title, String description, Integer gwangsan, List<Long> imageIds);
}
