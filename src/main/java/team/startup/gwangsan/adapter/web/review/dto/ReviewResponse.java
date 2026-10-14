package team.startup.gwangsan.adapter.web.review.dto;

import team.startup.gwangsan.adapter.web.image.dto.GetImageResponse;

import java.util.List;

public record ReviewResponse(
        Long reviewId,
        Long productId,
        String content,
        Integer light,
        String reviewerName,
        String targetName,
        List<GetImageResponse> imageUrls
) {}