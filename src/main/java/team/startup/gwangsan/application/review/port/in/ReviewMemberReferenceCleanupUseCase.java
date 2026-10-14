package team.startup.gwangsan.application.review.port.in;

/**
 * 회원 탈퇴 시 후기의 회원 참조를 dummy 회원으로 재지정한다(member 탈퇴 cleanup 소비).
 * bulk UPDATE 이고 실행 뒤 영속성 컨텍스트를 비운다(clearAutomatically). 기존 순서: 작성자 → 대상.
 */
public interface ReviewMemberReferenceCleanupUseCase {

    /** tbl_review.reviewer_id 다음 reviewed_id */
    void reassign(Long memberId, Long dummyMemberId);
}
