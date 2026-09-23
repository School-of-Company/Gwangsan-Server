package team.startup.gwangsan.domain.post.service;

import java.time.LocalDateTime;

public interface ReservationProductService {
    void execute(Long productId, Long roomId, LocalDateTime scheduledAt, String placeName, String address, Double latitude, Double longitude);
}
