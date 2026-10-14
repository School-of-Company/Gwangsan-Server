package team.startup.gwangsan.application.member.data;

/** banned=true 는 관리자 강제 탈퇴(재가입 차단), false 는 본인 탈퇴. */
public record WithdrawMemberCommand(Long memberId, boolean banned) {
}
