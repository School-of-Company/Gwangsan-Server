package team.startup.gwangsan.application.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.feedback.data.FeedbackSummary;
import team.startup.gwangsan.application.feedback.data.MyFeedbackSearch;
import team.startup.gwangsan.application.feedback.port.in.FindMyFeedbacksQuery;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindMyFeedbacksService implements FindMyFeedbacksQuery {

    private final LoadFeedbackPort loadFeedbackPort;

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackSummary> query(MyFeedbackSearch search) {
        return loadFeedbackPort.findMySummaries(search.actor().memberId(), search.lastId(), search.size());
    }
}
