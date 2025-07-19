package team.startup.gwangsan.domain.post.service;

import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductByIdResponse;

public interface FindProductByIdService {
    GetProductByIdResponse execute(Long id);
}
