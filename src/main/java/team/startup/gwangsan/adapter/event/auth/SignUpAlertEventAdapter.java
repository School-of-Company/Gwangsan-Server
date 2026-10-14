package team.startup.gwangsan.adapter.event.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.auth.port.out.SignUpAlertPort;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.application.admin.data.CreateAdminAlertEvent;
import team.startup.gwangsan.adapter.event.alert.CreateAlertEvent;

// 가입 알림은 원자 저장 대상이 아니다(계획 "1차 구현 검토 확정: 알림 저장 범위"). 기존 AFTER_COMMIT·async 이벤트를 유지한다.
// CreateAlertEvent 는 PR04 가 adapter/event/alert 로 옮겼다. CreateAdminAlertEvent 는 PR11 이 옮기면 import 만 바꾼다.
@Component
@RequiredArgsConstructor
public class SignUpAlertEventAdapter implements SignUpAlertPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void notifySignUp(Long memberId, Long recommenderId) {
        applicationEventPublisher.publishEvent(new CreateAdminAlertEvent(AlertType.SIGN_UP, memberId, memberId));
        applicationEventPublisher.publishEvent(new CreateAlertEvent(memberId, recommenderId,
                team.startup.gwangsan.domain.alert.model.AlertType.RECOMMENDER));
    }
}
