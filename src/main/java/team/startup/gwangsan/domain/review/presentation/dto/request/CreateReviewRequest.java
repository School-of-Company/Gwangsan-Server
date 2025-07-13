package team.startup.gwangsan.domain.review.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReviewRequest(
        @NotNull Long productId,
        @NotBlank String content,
        @Min(0) @Max(100) Integer light
) {}
