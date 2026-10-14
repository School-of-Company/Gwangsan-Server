package team.startup.gwangsan.adapter.persistence.review;

public record ReceivedReviewDto(
        Long reviewId,
        Long productId,
        String content,
        Integer light,
        String reviewerNickname
) {}