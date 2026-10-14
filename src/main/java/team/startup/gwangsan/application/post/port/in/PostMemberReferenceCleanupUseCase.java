package team.startup.gwangsan.application.post.port.in;

/**
 * 회원 탈퇴 시 게시글 업무의 회원 참조를 dummy 회원으로 재지정한다(member 탈퇴 cleanup 소비).
 * 각 메서드는 즉시 실행되는 bulk UPDATE 이고 실행 뒤 영속성 컨텍스트를 비운다(clearAutomatically).
 * 기존 순서: 게시글 작성자 재지정 직후 예약자 재지정.
 */
public interface PostMemberReferenceCleanupUseCase {

    /** tbl_product.member_id */
    void reassignAuthor(Long memberId, Long dummyMemberId);

    /** tbl_product_reservation.reserver_id */
    void reassignReserver(Long memberId, Long dummyMemberId);
}
