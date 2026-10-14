package team.startup.gwangsan.application.suspend.port.out;

public interface RequestSuspensionAlertPort {

    /** 신고 처리 결과 회원 알림 요청. 기존과 같이 커밋 후 비동기로 처리된다. */
    void requestReportAlert(Long reportId, Long memberId, Long suspendId);
}
