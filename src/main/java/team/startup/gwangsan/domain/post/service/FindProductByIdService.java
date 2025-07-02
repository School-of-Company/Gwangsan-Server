package team.startup.gwangsan.domain.post.service;

import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;

public interface FindProductByIdService {
    GetProductResponse execute(Long id);
}
