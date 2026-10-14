package team.startup.gwangsan.adapter.integration.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import team.startup.gwangsan.application.admin.port.in.ReassignAdminAlertMemberUseCase;
import team.startup.gwangsan.application.alert.port.in.AlertMemberCleanupUseCase;
import team.startup.gwangsan.application.block.port.in.DeleteMemberBlocksUseCase;
import team.startup.gwangsan.application.chat.port.in.ChatMemberCleanupUseCase;
import team.startup.gwangsan.application.notice.port.in.ReassignNoticeAuthorUseCase;
import team.startup.gwangsan.application.post.port.in.PostMemberReferenceCleanupUseCase;
import team.startup.gwangsan.application.relatedkeyword.port.in.DeleteAllMemberRelatedKeywordsUseCase;
import team.startup.gwangsan.application.report.port.in.ReassignReportMemberUseCase;
import team.startup.gwangsan.application.review.port.in.ReviewMemberReferenceCleanupUseCase;
import team.startup.gwangsan.application.suspend.port.in.DeleteMemberSuspensionsUseCase;
import team.startup.gwangsan.application.trade.port.in.TradeMemberReferenceCleanupUseCase;

import static org.mockito.Mockito.*;

@DisplayName("MemberReferenceCleanupAdapter 단위 테스트")
class MemberReferenceCleanupAdapterTest {
    private final PostMemberReferenceCleanupUseCase post = mock(PostMemberReferenceCleanupUseCase.class);
    private final TradeMemberReferenceCleanupUseCase trade = mock(TradeMemberReferenceCleanupUseCase.class);
    private final ChatMemberCleanupUseCase chat = mock(ChatMemberCleanupUseCase.class);
    private final ReviewMemberReferenceCleanupUseCase review = mock(ReviewMemberReferenceCleanupUseCase.class);
    private final ReassignReportMemberUseCase report = mock(ReassignReportMemberUseCase.class);
    private final ReassignAdminAlertMemberUseCase adminAlerts = mock(ReassignAdminAlertMemberUseCase.class);
    private final AlertMemberCleanupUseCase alerts = mock(AlertMemberCleanupUseCase.class);
    private final ReassignNoticeAuthorUseCase notice = mock(ReassignNoticeAuthorUseCase.class);
    private final DeleteAllMemberRelatedKeywordsUseCase keywords = mock(DeleteAllMemberRelatedKeywordsUseCase.class);
    private final DeleteMemberSuspensionsUseCase suspensions = mock(DeleteMemberSuspensionsUseCase.class);
    private final DeleteMemberBlocksUseCase blocks = mock(DeleteMemberBlocksUseCase.class);
    private final MemberReferenceCleanupAdapter adapter = new MemberReferenceCleanupAdapter(post, trade, chat, review,
            report, adminAlerts, alerts, notice, keywords, suspensions, blocks);

    @Test
    @DisplayName("it_게시글 작성자 재지정 직후 예약자를 재지정하고, 나머지 업무 참조를 기존 순서로 재지정한다")
    void it_reassigns_in_existing_order() {
        adapter.reassignReferences(1L, 99L);

        InOrder order = inOrder(post, trade, chat, review, report, adminAlerts, alerts, notice);
        order.verify(post).reassignAuthor(1L, 99L);
        order.verify(post).reassignReserver(1L, 99L);
        order.verify(trade).reassignTradeParties(1L, 99L);
        order.verify(chat).reassignMember(1L, 99L);
        order.verify(review).reassign(1L, 99L);
        order.verify(report).reassign(1L, 99L);
        order.verify(adminAlerts).reassign(1L, 99L);
        order.verify(trade).reassignCancelRequester(1L, 99L);
        order.verify(alerts).reassignSender(1L, 99L);
        order.verify(notice).reassign(1L, 99L);
        verifyNoInteractions(keywords, suspensions, blocks);
    }

    @Test
    @DisplayName("it_관심 키워드는 탈퇴 전용 즉시 bulk 삭제를 첫 단계로, 정지·차단·알림 수신 순으로 지운다")
    void it_deletes_personal_data_in_existing_order() {
        adapter.deletePersonalData(1L);

        InOrder order = inOrder(keywords, suspensions, blocks, alerts);
        order.verify(keywords).execute(1L);
        order.verify(suspensions).deleteAllOf(1L);
        order.verify(blocks).deleteAllOf(1L);
        order.verify(alerts).deleteReceipts(1L);
    }
}
