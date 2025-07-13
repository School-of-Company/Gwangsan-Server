package team.startup.gwangsan.domain.review.service;

import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;

import java.util.List;

public interface GetMyReviewListService {
    List<ReviewResponse> execute(Type type, Mode mode);
}
