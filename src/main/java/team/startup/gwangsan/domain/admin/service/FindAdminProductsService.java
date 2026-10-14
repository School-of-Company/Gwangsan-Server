package team.startup.gwangsan.domain.admin.service;

import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.adapter.web.post.dto.GetProductResponse;

import java.util.List;

public interface FindAdminProductsService {
    List<GetProductResponse> execute(Type type, Mode mode, Long lastId, int size);
}
