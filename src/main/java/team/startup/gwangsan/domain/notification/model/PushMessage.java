package team.startup.gwangsan.domain.notification.model;

public record PushMessage(
        String title,
        String body,
        NotificationType type,
        Long sourceId
) {
    private static final String DEFAULT_TITLE = "시민화폐 광산";
    // 예약 알림은 기존부터 쉼표가 들어간 제목을 쓴다.
    private static final String RESERVATION_TITLE = "시민화폐, 광산";

    private static final String NOTICE_BODY = "새로운 공지가 등록되었습니다.";
    private static final String CHAT_BODY = "새로운 메세지가 도착했습니다.";
    private static final String RECOMMENDATION_BODY = "추천인이 등록되었습니다.";
    private static final String TRADE_COMPLETE_BODY = "거래가 승인되었습니다.";

    public static PushMessage byType(NotificationType type, Long sourceId) {
        String body = switch (type) {
            case NOTICE -> NOTICE_BODY;
            case CHATTING -> CHAT_BODY;
            case RECOMMENDATION -> RECOMMENDATION_BODY;
            case TRADE_COMPLETE -> TRADE_COMPLETE_BODY;
            case RESERVATION_REMINDER, RESERVATION_CANCEL -> throw new IllegalArgumentException("예약 알림은 상대방 이름이 필요합니다.");
        };
        return new PushMessage(DEFAULT_TITLE, body, type, sourceId);
    }

    public static PushMessage reservation(NotificationType type, Long roomId, String body) {
        return new PushMessage(RESERVATION_TITLE, body, type, roomId);
    }
}
