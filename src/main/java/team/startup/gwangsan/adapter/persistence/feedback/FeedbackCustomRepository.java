package team.startup.gwangsan.adapter.persistence.feedback;

import com.querydsl.core.Tuple;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

import java.util.List;
import java.util.Optional;

public interface FeedbackCustomRepository {

    List<FeedbackJpaEntity> findMyFeedbacks(Long memberId, Long lastId, int size);

    /** 각 행은 (FeedbackJpaEntity, 작성자 닉네임, 지점 ID, 지점명, 답변자 닉네임) 순서다. */
    List<Tuple> findAdminRows(FeedbackStatus status, FeedbackType type, Integer strictPlaceId,
                               Integer scopePlaceIdExemptingBug, Long lastId, int size);

    Optional<Tuple> findRowById(Long feedbackId);

    /** (유형, 작성자 지점 ID) 순서다. */
    Optional<Tuple> findAccessRow(Long feedbackId);
}
