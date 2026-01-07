package team.startup.gwangsan.domain.review.repository.projection;

public record MyReviewDto(
        Long reviewId,
        Long productId,
        String content,
        Integer light
) {}