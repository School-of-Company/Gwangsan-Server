package team.startup.gwangsan.adapter.integration.member;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.block.MemberBlockJpaRepository;
import team.startup.gwangsan.adapter.persistence.chat.ChatMessageJpaRepository;
import team.startup.gwangsan.adapter.persistence.chat.ChatRoomJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.notice.NoticeJpaRepository;
import team.startup.gwangsan.adapter.persistence.report.ReportJpaRepository;
import team.startup.gwangsan.adapter.persistence.review.ReviewJpaRepository;
import team.startup.gwangsan.adapter.persistence.suspend.SuspendJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaRepository;
import team.startup.gwangsan.application.admin.port.in.ReassignAdminAlertMemberUseCase;
import team.startup.gwangsan.application.alert.port.in.AlertMemberCleanupUseCase;
import team.startup.gwangsan.application.member.port.out.MemberReferenceCleanupPort;
import team.startup.gwangsan.application.post.port.in.PostMemberReferenceCleanupUseCase;
import team.startup.gwangsan.application.relatedkeyword.port.in.DeleteAllMemberRelatedKeywordsUseCase;

/**
 * 회원 탈퇴 시 다른 업무의 회원 참조 정리. 순서는 기존 MemberDeletionServiceImpl + fix/member-withdrawal-reference-cleanup 와 같다.
 * 공개 Port: post(작성자 → 직후 예약자), relatedkeyword(탈퇴 전용 즉시 bulk 삭제), alert(발신자·수신 기록), admin(관리자 알림).
 * legacy bridge: 회원 정리 공개 Port 가 없는 trade·review(PR07)·chat(PR08)·report·block·suspend(PR09)·notice(PR10) 는
 * 각 저장 모델 Repository 의 기존 bulk 쿼리(clearAutomatically)를 그대로 호출한다. 제거: 각 업무 회원 정리 Port → PR12.
 * 프로필 수정 경로의 파생 키워드 삭제는 이 클래스와 무관하다(dirty 변경 보존).
 */
@Component
@RequiredArgsConstructor
public class MemberReferenceCleanupAdapter implements MemberReferenceCleanupPort {

    private final EntityManager entityManager;
    private final PostMemberReferenceCleanupUseCase postMemberReferenceCleanupUseCase;
    private final TradeCompleteJpaRepository tradeCompleteRepository;
    private final ChatRoomJpaRepository chatRoomRepository;
    private final ChatMessageJpaRepository chatMessageRepository;
    private final ReviewJpaRepository reviewRepository;
    private final ReportJpaRepository reportRepository;
    private final ReassignAdminAlertMemberUseCase reassignAdminAlertMemberUseCase;
    private final TradeCancelJpaRepository tradeCancelRepository;
    private final AlertMemberCleanupUseCase alertMemberCleanupUseCase;
    private final NoticeJpaRepository noticeRepository;
    private final DeleteAllMemberRelatedKeywordsUseCase deleteAllMemberRelatedKeywordsUseCase;
    private final SuspendJpaRepository suspendRepository;
    private final MemberBlockJpaRepository memberBlockRepository;

    @Override
    public void reassignReferences(Long memberId, Long dummyMemberId) {
        postMemberReferenceCleanupUseCase.reassignAuthor(memberId, dummyMemberId);
        postMemberReferenceCleanupUseCase.reassignReserver(memberId, dummyMemberId);
        // 앞 단계의 clearAutomatically 로 컨텍스트가 비므로, 이후 참조는 매번 새로 얻는다.
        tradeCompleteRepository.reassignBuyer(reference(memberId), reference(dummyMemberId));
        tradeCompleteRepository.reassignSeller(reference(memberId), reference(dummyMemberId));
        chatRoomRepository.reassignBuyer(reference(memberId), reference(dummyMemberId));
        chatRoomRepository.reassignSeller(reference(memberId), reference(dummyMemberId));
        chatMessageRepository.reassignSender(reference(memberId), reference(dummyMemberId));
        reviewRepository.reassignReviewer(reference(memberId), reference(dummyMemberId));
        reviewRepository.reassignReviewed(reference(memberId), reference(dummyMemberId));
        reportRepository.reassignReporter(reference(memberId), reference(dummyMemberId));
        reportRepository.reassignReported(reference(memberId), reference(dummyMemberId));
        reassignAdminAlertMemberUseCase.reassign(memberId, dummyMemberId);
        tradeCancelRepository.reassignMember(reference(memberId), reference(dummyMemberId));
        alertMemberCleanupUseCase.reassignSender(memberId, dummyMemberId);
        noticeRepository.reassignMember(reference(memberId), reference(dummyMemberId));
    }

    @Override
    public void deletePersonalData(Long memberId) {
        deleteAllMemberRelatedKeywordsUseCase.execute(memberId);
        suspendRepository.deleteAllByMember(reference(memberId));
        memberBlockRepository.deleteAllByBlockerOrBlocked(reference(memberId));
        alertMemberCleanupUseCase.deleteReceipts(memberId);
    }

    private MemberJpaEntity reference(Long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }
}
