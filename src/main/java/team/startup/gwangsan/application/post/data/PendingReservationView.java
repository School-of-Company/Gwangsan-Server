package team.startup.gwangsan.application.post.data;

import java.time.LocalDateTime;

/** 진행 중(PENDING) 예약의 공개 읽기 값. createdAt 은 예약 요청 시각이다. */
public record PendingReservationView(
        Long id,
        Long productId,
        Long reserverId,
        LocalDateTime scheduledAt,
        LocalDateTime createdAt,
        String placeName,
        String address,
        Double latitude,
        Double longitude
) {
}
