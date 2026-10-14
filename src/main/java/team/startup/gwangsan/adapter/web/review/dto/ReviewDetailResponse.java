package team.startup.gwangsan.adapter.web.review.dto;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.util.List;

public record ReviewDetailResponse(
        Long reviewId,
        Long productId,
        String title,
        String content,
        Integer light,
        List<GetImageResponse> imageUrls

) {
}
