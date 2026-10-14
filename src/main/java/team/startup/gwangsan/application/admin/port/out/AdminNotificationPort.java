package team.startup.gwangsan.application.admin.port.out;

import team.startup.gwangsan.application.admin.data.MemberAlertKind;

/** 관리자 처리 결과 회원 알림. 기존처럼 커밋 후 비동기 생성 요청으로 전달한다. */
public interface AdminNotificationPort {
    void notifyMember(Long sourceId, Long memberId, MemberAlertKind kind);
}
