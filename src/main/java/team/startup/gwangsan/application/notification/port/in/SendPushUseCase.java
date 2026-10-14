package team.startup.gwangsan.application.notification.port.in;

import team.startup.gwangsan.application.notification.data.SendPushCommand;
import team.startup.gwangsan.domain.notification.model.PushOutcome;

/**
 * 회원 ID 로 푸시를 보낸다. 기기 토큰과 Expo 규격은 호출자에게 드러나지 않는다.
 * 일반 푸시는 업무 커밋 후 호출한다. 발송 확인이 필요한 유형(예약 리마인더)은 최종 실패 시
 * PushDeliveryFailedException 을 던진다.
 */
public interface SendPushUseCase {
    PushOutcome send(SendPushCommand command);
}
