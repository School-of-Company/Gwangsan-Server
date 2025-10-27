package team.startup.gwangsan.domain.review.service;

import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewDetailResponse;

public interface GetReviewDetailService {
    ReviewDetailResponse execute(Long reviewId);
}
