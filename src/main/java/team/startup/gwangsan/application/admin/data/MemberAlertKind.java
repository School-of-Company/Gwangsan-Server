package team.startup.gwangsan.application.admin.data;

// alert 의 AlertType 중 관리자 처리 결과로 회원에게 보내는 종류. 이름 기준으로 매핑한다.
public enum MemberAlertKind {
    REPORT_REJECT,
    TRADE_CANCEL_REJECT,
    RECOMMENDER
}
