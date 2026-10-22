package team.startup.gwangsan.adapter.persistence.feedback;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import static team.startup.gwangsan.adapter.persistence.feedback.QFeedbackImageJpaEntity.feedbackImageJpaEntity;

@Repository
@RequiredArgsConstructor
public class FeedbackImageCustomRepositoryImpl implements FeedbackImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<FeedbackImageJpaEntity> findAllByFeedbackIdIn(Collection<Long> feedbackIds) {
        return queryFactory.selectFrom(feedbackImageJpaEntity)
                .join(feedbackImageJpaEntity.image).fetchJoin()
                .where(feedbackImageJpaEntity.feedback.id.in(feedbackIds))
                .fetch();
    }
}
