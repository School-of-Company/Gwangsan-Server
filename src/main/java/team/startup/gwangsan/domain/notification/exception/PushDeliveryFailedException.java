package team.startup.gwangsan.domain.notification.exception;

import team.startup.gwangsan.domain.notification.model.NotificationType;

/**
 * 발송 확인이 필요한 유형의 푸시가 최종 실패했다. HTTP 로 나가지 않고 scheduler 가 기록한다.
 */
public class PushDeliveryFailedException extends RuntimeException {
    public PushDeliveryFailedException(NotificationType type, Long sourceId) {
        super("푸시 발송 실패: type=" + type + ", sourceId=" + sourceId);
    }
}
