package team.startup.gwangsan.domain.review.repository.custom;

public record MyReviewRow(
        Long reviewId,
        Long productId,
        String content,
        Integer light
) {}