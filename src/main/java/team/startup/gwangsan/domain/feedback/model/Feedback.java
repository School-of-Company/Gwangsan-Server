package team.startup.gwangsan.domain.feedback.model;

import lombok.Getter;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.time.LocalDateTime;

/** 문의 저장 전에는 id·createdAt 이 null 이다. 저장 후 상태 전이는 answer()/changeStatus() 로만 한다. */
@Getter
public class Feedback {

    private final Long id;
    private final Long memberId;
    private final FeedbackType type;
    private final String content;
    private final String appVersion;
    private final OsType osType;
    private final String osVersion;
    private final String deviceModel;
    private FeedbackStatus status;
    private String answer;
    private Long answeredByMemberId;
    private LocalDateTime answeredAt;
    private final LocalDateTime createdAt;

    private Feedback(Long id, Long memberId, FeedbackType type, String content, String appVersion, OsType osType,
                      String osVersion, String deviceModel, FeedbackStatus status, String answer,
                      Long answeredByMemberId, LocalDateTime answeredAt, LocalDateTime createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.type = type;
        this.content = content;
        this.appVersion = appVersion;
        this.osType = osType;
        this.osVersion = osVersion;
        this.deviceModel = deviceModel;
        this.status = status;
        this.answer = answer;
        this.answeredByMemberId = answeredByMemberId;
        this.answeredAt = answeredAt;
        this.createdAt = createdAt;
    }

    public static Feedback create(Long memberId, FeedbackType type, String content, String appVersion, OsType osType,
                                   String osVersion, String deviceModel) {
        return new Feedback(null, memberId, type, content, appVersion, osType, osVersion, deviceModel,
                FeedbackStatus.RECEIVED, null, null, null, null);
    }

    public static Feedback of(Long id, Long memberId, FeedbackType type, String content, String appVersion,
                               OsType osType, String osVersion, String deviceModel, FeedbackStatus status,
                               String answer, Long answeredByMemberId, LocalDateTime answeredAt,
                               LocalDateTime createdAt) {
        return new Feedback(id, memberId, type, content, appVersion, osType, osVersion, deviceModel, status, answer,
                answeredByMemberId, answeredAt, createdAt);
    }

    /** 답변을 저장하면 상태는 항상 DONE 으로 바뀐다. */
    public void answer(String answer, Long answeredByMemberId, LocalDateTime answeredAt) {
        this.answer = answer;
        this.answeredByMemberId = answeredByMemberId;
        this.answeredAt = answeredAt;
        this.status = FeedbackStatus.DONE;
    }

    public void changeStatus(FeedbackStatus status) {
        this.status = status;
    }
}
