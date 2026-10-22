package team.startup.gwangsan.application.feedback.port.in;

/** 회원 탈퇴 정리: 작성자·답변자 참조를 더미 회원으로 재지정한다(삭제가 아니다). */
public interface ReassignFeedbackMemberUseCase {
    void reassign(Long memberId, Long dummyMemberId);
}
