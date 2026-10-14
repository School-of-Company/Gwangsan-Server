package team.startup.gwangsan.adapter.integration.member;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
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
import team.startup.gwangsan.application.post.port.in.PostMemberReferenceCleanupUseCase;
import team.startup.gwangsan.application.relatedkeyword.port.in.DeleteAllMemberRelatedKeywordsUseCase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("MemberReferenceCleanupAdapter 단위 테스트")
class MemberReferenceCleanupAdapterTest {
    private final EntityManager em = mock(EntityManager.class);
    private final PostMemberReferenceCleanupUseCase post = mock(PostMemberReferenceCleanupUseCase.class);
    private final TradeCompleteJpaRepository tradeCompletes = mock(TradeCompleteJpaRepository.class);
    private final ReassignAdminAlertMemberUseCase adminAlerts = mock(ReassignAdminAlertMemberUseCase.class);
    private final AlertMemberCleanupUseCase alerts = mock(AlertMemberCleanupUseCase.class);
    private final NoticeJpaRepository notices = mock(NoticeJpaRepository.class);
    private final DeleteAllMemberRelatedKeywordsUseCase keywords = mock(DeleteAllMemberRelatedKeywordsUseCase.class);
    private final SuspendJpaRepository suspends = mock(SuspendJpaRepository.class);
    private final MemberBlockJpaRepository blocks = mock(MemberBlockJpaRepository.class);
    private final MemberReferenceCleanupAdapter adapter = new MemberReferenceCleanupAdapter(em, post, tradeCompletes,
            mock(ChatRoomJpaRepository.class), mock(ChatMessageJpaRepository.class), mock(ReviewJpaRepository.class),
            mock(ReportJpaRepository.class), adminAlerts, mock(TradeCancelJpaRepository.class), alerts, notices,
            keywords, suspends, blocks);

    @Test
    @DisplayName("it_게시글 작성자 재지정 직후 예약자를 재지정하고, 알림·공지까지 기존 순서로 재지정한다")
    void it_reassigns_reserver_right_after_author() {
        when(em.getReference(any(), any())).thenReturn(mock(MemberJpaEntity.class));

        adapter.reassignReferences(1L, 99L);

        InOrder order = inOrder(post, tradeCompletes, adminAlerts, alerts, notices);
        order.verify(post).reassignAuthor(1L, 99L);
        order.verify(post).reassignReserver(1L, 99L);
        order.verify(tradeCompletes).reassignBuyer(any(), any());
        order.verify(adminAlerts).reassign(1L, 99L);
        order.verify(alerts).reassignSender(1L, 99L);
        order.verify(notices).reassignMember(any(), any());
    }

    @Test
    @DisplayName("it_관심 키워드는 탈퇴 전용 즉시 bulk 삭제 Port 를 첫 단계로 쓰고, 수신 기록은 alert Port 로 마지막에 지운다")
    void it_bulk_deletes_keywords_first() {
        when(em.getReference(any(), any())).thenReturn(mock(MemberJpaEntity.class));

        adapter.deletePersonalData(1L);

        InOrder order = inOrder(keywords, suspends, blocks, alerts);
        order.verify(keywords).execute(1L);
        order.verify(suspends).deleteAllByMember(any());
        order.verify(blocks).deleteAllByBlockerOrBlocked(any());
        order.verify(alerts).deleteReceipts(1L);
    }
}
