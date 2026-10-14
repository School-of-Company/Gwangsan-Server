package team.startup.gwangsan.application.post.port.in;

public interface SendReservationReminderUseCase {
    /**
     * 예약 1건의 기한이 된 알림을 동기 발송한다. 발송이 성공한 뒤에만 sentAt 을 기록하며,
     * 발송 실패는 예외로 전파되어 기록 없이 rollback 된다(다음 주기에 재시도).
     */
    void execute(Long reservationId);
}
