package team.startup.gwangsan.domain.post.presentation.dto.response;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;

import java.util.List;

public record GetProductResponse(
        Long id,
        String title,
        String content,
        Integer gwangsan,
        Type type,
        Mode mode,
        List<GetImageResponse> images
) {
}
