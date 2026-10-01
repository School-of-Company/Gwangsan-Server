package team.startup.gwangsan.global.event;

import java.time.LocalDateTime;

public record ReservationCancelledEvent(Long roomId, Long messageId, Long senderId,
                                        Long recipientId, String content, LocalDateTime createdAt) {
}
