package team.startup.gwangsan.application.report.port.out;

public interface RequestReportAlertPort {

    /** 관리자 신고 알림 요청. 기존과 같이 커밋 후 비동기로 처리된다. */
    void requestAdminAlert(Long reportId, Long reporterId);
}
