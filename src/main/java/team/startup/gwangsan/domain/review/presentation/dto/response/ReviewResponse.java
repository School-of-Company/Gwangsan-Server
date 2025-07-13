package team.startup.gwangsan.domain.review.presentation.dto.response;

public record ReviewResponse(
        Long productId,
        String content,
        Integer light,
        String reviewerName
) {}
