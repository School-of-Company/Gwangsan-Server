package team.startup.gwangsan.domain.post.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;

import java.util.List;

public record PatchProductRequest(
        @NotNull Type type,
        @NotNull Mode mode,
        @NotNull @Max(20) String title,
        @NotNull String description,
        @NotNull Integer gwangsan,
        @NotNull List<Long> imageIds
) {
}
