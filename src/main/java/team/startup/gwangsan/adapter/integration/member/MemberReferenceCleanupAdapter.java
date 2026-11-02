package team.startup.gwangsan.adapter.integration.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.admin.port.in.ReassignAdminAlertMemberUseCase;
import team.startup.gwangsan.application.alert.port.in.AlertMemberCleanupUseCase;
import team.startup.gwangsan.application.block.port.in.DeleteMemberBlocksUseCase;
import team.startup.gwangsan.application.chat.port.in.ChatMemberCleanupUseCase;
import team.startup.gwangsan.application.member.port.out.MemberReferenceCleanupPort;
import team.startup.gwangsan.application.feedback.port.in.FeedbackCleanupUseCase;
import team.startup.gwangsan.application.notice.port.in.ReassignNoticeAuthorUseCase;
import team.startup.gwangsan.application.post.port.in.PostMemberReferenceCleanupUseCase;
import team.startup.gwangsan.application.relatedkeyword.port.in.DeleteAllMemberRelatedKeywordsUseCase;
import team.startup.gwangsan.application.report.port.in.ReassignReportMemberUseCase;
import team.startup.gwangsan.application.review.port.in.ReviewMemberReferenceCleanupUseCase;
import team.startup.gwangsan.application.suspend.port.in.DeleteMemberSuspensionsUseCase;
import team.startup.gwangsan.application.trade.port.in.TradeMemberReferenceCleanupUseCase;

/**
 * 회원 탈퇴 시 다른 업무의 회원 참조 정리. 순서는 기존 MemberDeletionServiceImpl + fix/member-withdrawal-reference-cleanup 와 같다.
 * 모두 각 업무의 탈퇴 정리 공개 command 이며 호출자(탈퇴) 트랜잭션에 참여한다. 각 단계는 즉시 실행되는 bulk 쿼리이고
 * 실행 뒤 영속성 컨텍스트를 비운다(clearAutomatically). 프로필 수정 경로의 파생 키워드 삭제는 이 클래스와 무관하다.
 */
@Component
@RequiredArgsConstructor
public class MemberReferenceCleanupAdapter implements MemberReferenceCleanupPort {

    private final FeedbackCleanupUseCase feedbackCleanupUseCase;

    private final PostMemberReferenceCleanupUseCase postMemberReferenceCleanupUseCase;
    private final TradeMemberReferenceCleanupUseCase tradeMemberReferenceCleanupUseCase;
    private final ChatMemberCleanupUseCase chatMemberCleanupUseCase;
    private final ReviewMemberReferenceCleanupUseCase reviewMemberReferenceCleanupUseCase;
    private final ReassignReportMemberUseCase reassignReportMemberUseCase;
    private final ReassignAdminAlertMemberUseCase reassignAdminAlertMemberUseCase;
    private final AlertMemberCleanupUseCase alertMemberCleanupUseCase;
    private final ReassignNoticeAuthorUseCase reassignNoticeAuthorUseCase;
    private final DeleteAllMemberRelatedKeywordsUseCase deleteAllMemberRelatedKeywordsUseCase;
    private final DeleteMemberSuspensionsUseCase deleteMemberSuspensionsUseCase;
    private final DeleteMemberBlocksUseCase deleteMemberBlocksUseCase;

    @Override
    public void reassignReferences(Long memberId, Long dummyMemberId) {
        postMemberReferenceCleanupUseCase.reassignAuthor(memberId, dummyMemberId);
        postMemberReferenceCleanupUseCase.reassignReserver(memberId, dummyMemberId);
        tradeMemberReferenceCleanupUseCase.reassignTradeParties(memberId, dummyMemberId);   // 구매자 → 판매자
        chatMemberCleanupUseCase.reassignMember(memberId, dummyMemberId);                  // 방 구매자 → 판매자 → 메시지 발신자
        reviewMemberReferenceCleanupUseCase.reassign(memberId, dummyMemberId);             // 작성자 → 대상
        reassignReportMemberUseCase.reassign(memberId, dummyMemberId);                     // 신고자 → 피신고자
        feedbackCleanupUseCase.reassign(memberId, dummyMemberId);
        reassignAdminAlertMemberUseCase.reassign(memberId, dummyMemberId);                 // 상대 → 요청자
        tradeMemberReferenceCleanupUseCase.reassignCancelRequester(memberId, dummyMemberId);
        alertMemberCleanupUseCase.reassignSender(memberId, dummyMemberId);
        reassignNoticeAuthorUseCase.reassign(memberId, dummyMemberId);
    }

    @Override
    public void deletePersonalData(Long memberId) {
        deleteAllMemberRelatedKeywordsUseCase.execute(memberId);
        deleteMemberSuspensionsUseCase.deleteAllOf(memberId);
        deleteMemberBlocksUseCase.deleteAllOf(memberId);
        alertMemberCleanupUseCase.deleteReceipts(memberId);
    }
}
