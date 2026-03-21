package team.startup.gwangsan.domain.post.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;

import java.util.List;

public record CreateProductRequest(
        @NotNull Type type,
        @NotNull Mode mode,
        @NotNull @Size(max = 20) String title,
        @NotNull String content,
        @NotNull Integer gwangsan,
        List<Long> imageIds
) {
}
