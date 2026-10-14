package team.startup.gwangsan.adapter.persistence.review;

public record MyReviewDto(
        Long reviewId,
        Long productId,
        String content,
        Integer light,
        String reviewedNickname
) {}