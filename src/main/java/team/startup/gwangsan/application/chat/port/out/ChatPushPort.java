package team.startup.gwangsan.application.chat.port.out;

public interface ChatPushPort {
    /** 새 채팅 푸시. 실제 발송은 커밋 후이며 수신 기기 조회는 알림 쪽 책임이다. */
    void notifyNewMessage(Long recipientId, Long roomId);
}
