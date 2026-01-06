package team.startup.gwangsan.domain.review.repository.custom;

public record ReceivedReviewRow(
        Long reviewId,
        Long productId,
        String content,
        Integer light,
        String reviewerNickname
) {}