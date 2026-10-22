package team.startup.gwangsan.application.feedback.port.in;

import team.startup.gwangsan.application.feedback.data.AdminFeedbackSearch;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;

import java.util.List;

public interface FindAdminFeedbacksQuery {
    List<FeedbackDetail> query(AdminFeedbackSearch search);
}
