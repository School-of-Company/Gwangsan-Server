package team.startup.gwangsan.domain.notification.model;

public enum NotificationType {
    NOTICE,
    CHATTING,
    RECOMMENDATION,
    TRADE_COMPLETE,
    RESERVATION_REMINDER,
    RESERVATION_CANCEL;

    /**
     * 발송 실패를 호출자에게 알려야 하는 유형. 예약 리마인더는 발송 성공 후에만 sentAt 을 기록한다.
     */
    public boolean requiresDeliveryConfirmation() {
        return this == RESERVATION_REMINDER;
    }
}
