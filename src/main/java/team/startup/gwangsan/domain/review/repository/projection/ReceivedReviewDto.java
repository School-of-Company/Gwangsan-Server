package team.startup.gwangsan.domain.review.repository.projection;

public record ReceivedReviewDto(
        Long reviewId,
        Long productId,
        String content,
        Integer light,
        String reviewerNickname
) {}