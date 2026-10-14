package team.startup.gwangsan.domain.alert.model;

public enum AlertType {
    NOTICE,
    TRADE_COMPLETE,
    TRADE_COMPLETE_REJECT,
    OTHER_MEMBER_TRADE_COMPLETE,
    RECOMMENDER,
    REVIEW,
    TRADE_CANCEL,
    TRADE_CANCEL_REJECT,
    REPORT,
    REPORT_REJECT;

    /**
     * 업무 변경과 같은 DB 트랜잭션에 저장하는 알림. 1차 구현 검토에서 거래 완료 요청과 철회 완료 두 가지로 확정했다.
     * 나머지(가입·신고·정지·후기·공지 등)는 커밋 후 비동기 저장을 유지한다.
     */
    public boolean isRecordedWithBusinessTransaction() {
        return this == OTHER_MEMBER_TRADE_COMPLETE || this == TRADE_CANCEL;
    }
}
