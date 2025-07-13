package team.startup.gwangsan.domain.review.service;

import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;

import java.util.List;

public interface GetMyReviewListService {
    List<ReviewResponse> execute(String type, String mode);
}
