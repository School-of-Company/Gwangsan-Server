package team.startup.gwangsan.adapter.integration.review;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.event.alert.CreateAlertEvent;
import team.startup.gwangsan.application.review.port.out.ReviewAlertPort;
import team.startup.gwangsan.domain.alert.model.AlertType;

/** 후기 등록 알림: 확정 정책대로 원자 저장 대상이 아니므로 기존 CreateAlertEvent(커밋 후 비동기)를 유지한다. */
@Component
@RequiredArgsConstructor
public class ReviewAlertAdapter implements ReviewAlertPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void notifyReviewed(long reviewId, long reviewedId) {
        applicationEventPublisher.publishEvent(new CreateAlertEvent(reviewId, reviewedId, AlertType.REVIEW));
    }
}
