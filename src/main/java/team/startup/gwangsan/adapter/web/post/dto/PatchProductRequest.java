package team.startup.gwangsan.adapter.web.post.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.List;

public record PatchProductRequest(
        @NotNull Type type,
        @NotNull Mode mode,
        @NotNull @Size(max = 20) String title,
        @NotNull String content,
        @NotNull Integer gwangsan,
        List<Long> imageIds
) {
}
