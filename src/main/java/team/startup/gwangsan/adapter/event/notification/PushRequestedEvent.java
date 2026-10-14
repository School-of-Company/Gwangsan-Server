package team.startup.gwangsan.adapter.event.notification;

import team.startup.gwangsan.domain.notification.model.NotificationType;

import java.util.List;

/**
 * 업무 트랜잭션 안에서 발행하고 커밋 후 발송하는 푸시 요청. 기기 토큰 대신 회원 ID 만 담는다.
 * 각 업무의 output Port 구현(adapter/integration)이 발행한다.
 */
public record PushRequestedEvent(
        List<Long> recipientMemberIds,
        NotificationType type,
        Long sourceId
) {
}
