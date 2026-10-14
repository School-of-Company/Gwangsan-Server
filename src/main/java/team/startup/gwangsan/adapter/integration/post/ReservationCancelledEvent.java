package team.startup.gwangsan.adapter.integration.post;

import java.time.LocalDateTime;

/**
 * 예약 취소 사실. 커밋 후 푸시와 채팅 서버 시스템 메시지 전달에만 쓰인다.
 */
public record ReservationCancelledEvent(Long roomId, Long messageId, Long senderId,
                                        Long recipientId, String content, LocalDateTime createdAt) {
}
