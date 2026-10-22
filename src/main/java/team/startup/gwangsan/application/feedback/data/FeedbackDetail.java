package team.startup.gwangsan.application.feedback.data;

import java.util.List;
import team.startup.gwangsan.domain.feedback.model.Feedback;

public record FeedbackDetail(Feedback feedback, String nickname, Integer placeId, String placeName,
                             String answeredBy, List<FeedbackImage> images) {
    public record FeedbackImage(Long imageId, String imageUrl) {}
}
