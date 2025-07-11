package team.startup.gwangsan.domain.post.service;

import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;

import java.util.List;

public interface FindProductsByMemberIdService {
    List<GetProductResponse> execute(Long memberId, Type type, Mode mode);
}
