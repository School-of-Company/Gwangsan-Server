package team.startup.gwangsan.application.suspend.data;

/** alertId 는 기존 계약상 신고 ID 이며, 같은 ID 의 관리자 알림이 없을 때만 회원 알림을 요청한다. */
public record SuspendMemberCommand(Long memberId, int suspendedDays, Long alertId) {
}
