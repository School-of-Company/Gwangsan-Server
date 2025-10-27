package team.startup.gwangsan.domain.review.presentation.dto.response;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.util.List;

public record ReviewDetailResponse(
        Long reviewId,
        String title,
        String content,
        Integer light,
        List<GetImageResponse> imageUrls

) {
}
