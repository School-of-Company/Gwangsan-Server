package team.startup.gwangsan.application.feedback.port.in;

import team.startup.gwangsan.application.feedback.data.FeedbackSummary;
import team.startup.gwangsan.application.feedback.data.MyFeedbackSearch;

import java.util.List;

public interface FindMyFeedbacksQuery {
    List<FeedbackSummary> query(MyFeedbackSearch search);
}
