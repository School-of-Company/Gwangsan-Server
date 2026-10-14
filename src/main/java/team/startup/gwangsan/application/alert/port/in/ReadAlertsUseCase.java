package team.startup.gwangsan.application.alert.port.in;

public interface ReadAlertsUseCase {
    /** alertId 이하의 미확인 receipt 를 모두 읽음 처리한다. */
    void readUpTo(Long memberId, Long alertId);
}
