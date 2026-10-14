package team.startup.gwangsan.application.post.port.out;

import team.startup.gwangsan.application.post.data.ReservationCancellation;

public interface ReservationNotificationPort {
    /** 커밋 후 비동기로 상대에게 푸시와 채팅 서버 시스템 메시지를 전달한다. 실패는 기록만 한다. */
    void notifyCancelled(ReservationCancellation cancellation);

    /** 동기 발송. 실패하면 예외를 던진다(reminder 의 sentAt 기록 조건). */
    void sendReminder(Long recipientId, String title, String body, Long roomId);
}
