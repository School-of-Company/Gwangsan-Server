package team.startup.gwangsan.domain.post.model;

import lombok.Getter;
import team.startup.gwangsan.domain.post.model.ReservationStatus;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;

import java.time.LocalDateTime;

/**
 * 게시글 거래 약속. 시간은 호출자가 넘긴 now 로만 판단한다(기존 Asia/Seoul 기준은 application 이 유지).
 */
@Getter
public class ProductReservation {

    private static final long REMINDER_BEFORE_MINUTES = 30;

    private final Long id;
    private final Long productId;
    private final Long reserverId;
    private ReservationStatus status;
    private final LocalDateTime scheduledAt;
    private final String placeName;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private LocalDateTime reminder30SentAt;
    private LocalDateTime reminderAtSentAt;

    public ProductReservation(Long id, Long productId, Long reserverId, ReservationStatus status,
                              LocalDateTime scheduledAt, String placeName, String address,
                              Double latitude, Double longitude,
                              LocalDateTime reminder30SentAt, LocalDateTime reminderAtSentAt) {
        this.id = id;
        this.productId = productId;
        this.reserverId = reserverId;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.reminder30SentAt = reminder30SentAt;
        this.reminderAtSentAt = reminderAtSentAt;
    }

    /**
     * 약속이 30분 이내면 30분 전 알림을 보낼 시점이 지났으므로 보낸 것으로 표시한다.
     */
    public static ProductReservation reserve(Long productId, Long reserverId, LocalDateTime scheduledAt,
                                             String placeName, String address, Double latitude, Double longitude,
                                             LocalDateTime now) {
        LocalDateTime reminder30 = scheduledAt.isAfter(now.plusMinutes(REMINDER_BEFORE_MINUTES)) ? null : now;
        return new ProductReservation(null, productId, reserverId, ReservationStatus.PENDING, scheduledAt,
                placeName, address, latitude, longitude, reminder30, null);
    }

    public void cancel(Long actorId, Long productAuthorId) {
        if (!productAuthorId.equals(actorId) && !reserverId.equals(actorId)) {
            throw new ReservationParticipantOnlyException();
        }
        this.status = ReservationStatus.CANCELLED;
    }

    /** 거래 완료 시 예약도 끝난다. 참여자 검사는 {@link #requireParticipant} 로 먼저 한다. */
    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    /** 예약자가 거래 당사자(구매자·판매자) 중 한 명이어야 한다. */
    public void requireParticipant(Long buyerId, Long sellerId) {
        if (!reserverId.equals(buyerId) && !reserverId.equals(sellerId)) {
            throw new ReservationParticipantOnlyException();
        }
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    public boolean isReminder30Due(LocalDateTime now) {
        return reminder30SentAt == null && !now.isBefore(scheduledAt.minusMinutes(REMINDER_BEFORE_MINUTES));
    }

    public boolean isReminderAtDue(LocalDateTime now) {
        return reminderAtSentAt == null && !now.isBefore(scheduledAt);
    }

    public void markReminder30Sent(LocalDateTime sentAt) {
        this.reminder30SentAt = sentAt;
    }

    public void markReminderAtSent(LocalDateTime sentAt) {
        this.reminderAtSentAt = sentAt;
    }
}
