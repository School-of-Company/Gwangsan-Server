package team.startup.gwangsan.domain.review.presentation.dto.response;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.util.List;

public record ReviewResponse(
        Long productId,
        String content,
        Integer light,
        String reviewerName,
        List<GetImageResponse> imageUrls
) {}