package team.startup.gwangsan.domain.review.service;

import team.startup.gwangsan.domain.review.presentation.dto.request.CreateReviewRequest;

public interface CreateReviewService {
    void execute(CreateReviewRequest request);
}
