package team.startup.gwangsan.application.report.port.in;

/** 회원 탈퇴 정리: 신고자·피신고자 참조를 대체 회원으로 bulk 재지정한다. 호출자 트랜잭션에 참여한다. */
public interface ReassignReportMemberUseCase {
    void reassign(Long memberId, Long dummyMemberId);
}
