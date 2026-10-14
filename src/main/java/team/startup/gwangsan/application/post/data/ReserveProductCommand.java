package team.startup.gwangsan.application.post.data;

import java.time.LocalDateTime;

public record ReserveProductCommand(Long actorId, Long productId, Long roomId, LocalDateTime scheduledAt,
                                    String placeName, String address, Double latitude, Double longitude) {
}
