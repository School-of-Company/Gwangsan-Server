package team.startup.gwangsan.application.feedback.port.in;

import java.util.List;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.*;

public interface FeedbackQuery {
    List<FeedbackDetail> mine(Long memberId, Long lastId, int size);
    List<FeedbackDetail> list(ActorContext actor, FeedbackStatus status, FeedbackType type,
                              Integer placeId, Long lastId, int size);
    FeedbackDetail detail(ActorContext actor, Long id);
}
