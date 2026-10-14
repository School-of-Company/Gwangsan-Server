package team.startup.gwangsan.application.post.data;

import java.time.LocalDateTime;

public record ReservationCancellation(Long roomId, Long messageId, Long senderId, Long recipientId,
                                      String content, LocalDateTime createdAt) {
}
