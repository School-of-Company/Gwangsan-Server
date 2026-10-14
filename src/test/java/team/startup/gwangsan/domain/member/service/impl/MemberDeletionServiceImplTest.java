package team.startup.gwangsan.domain.member.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.adapter.persistence.alert.AlertReceiptJpaRepository;
import team.startup.gwangsan.adapter.persistence.alert.AlertJpaRepository;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.adapter.persistence.chat.ChatMessageJpaRepository;
import team.startup.gwangsan.adapter.persistence.chat.ChatRoomJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.exception.DummyMemberDeletionNotAllowedException;
import team.startup.gwangsan.domain.member.exception.NotFoundDummyMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.adapter.persistence.post.ProductRepository;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.MemberRelatedKeywordJpaRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.adapter.persistence.review.ReviewJpaRepository;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelJpaRepository;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompleteJpaRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberDeletionServiceImpl 단위 테스트")
class MemberDeletionServiceImplTest {

    @InjectMocks private MemberDeletionServiceImpl service;

    @Mock private MemberJpaRepository memberRepository;
    @Mock private ProductRepository productRepository;
    @Mock private TradeCompleteJpaRepository tradeCompleteRepository;
    @Mock private TradeCancelJpaRepository tradeCancelRepository;
    @Mock private ChatRoomJpaRepository chatRoomRepository;
    @Mock private ChatMessageJpaRepository chatMessageRepository;
    @Mock private ReviewJpaRepository reviewRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private AdminAlertRepository adminAlertRepository;
    @Mock private AlertJpaRepository alertRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private MemberRelatedKeywordJpaRepository memberRelatedKeywordRepository;
    @Mock private SuspendRepository suspendRepository;
    @Mock private MemberBlockRepository memberBlockRepository;
    @Mock private AlertReceiptJpaRepository alertReceiptRepository;
    @Mock private MemberDetailJpaRepository memberDetailRepository;

    @Nested
    @DisplayName("delete() 메서드는")
    class Describe_delete {

        @Nested
        @DisplayName("정상적인 멤버 삭제 요청일 때")
        class Context_with_valid_member {

            @Test
            @DisplayName("모든 참조를 재할당하고 멤버를 삭제한다")
            void it_reassigns_all_references_and_deletes_member() {
                MemberJpaEntity member = mock(MemberJpaEntity.class);
                MemberJpaEntity dummy = mock(MemberJpaEntity.class);

                when(member.getId()).thenReturn(1L);
                when(dummy.getId()).thenReturn(99L);
                when(memberRepository.findByNickname("__withdrawn__")).thenReturn(Optional.of(dummy));

                service.delete(member);

                verify(memberRepository).reassignRecommender(member, dummy);
                verify(productRepository).reassignMember(member, dummy);
                verify(tradeCompleteRepository).reassignBuyer(member, dummy);
                verify(tradeCompleteRepository).reassignSeller(member, dummy);
                verify(chatRoomRepository).reassignBuyer(member, dummy);
                verify(chatRoomRepository).reassignSeller(member, dummy);
                verify(chatMessageRepository).reassignSender(member, dummy);
                verify(reviewRepository).reassignReviewer(member, dummy);
                verify(reviewRepository).reassignReviewed(member, dummy);
                verify(reportRepository).reassignReporter(member, dummy);
                verify(reportRepository).reassignReported(member, dummy);
                verify(adminAlertRepository).reassignOtherMember(member, dummy);
                verify(adminAlertRepository).reassignRequester(member, dummy);
                verify(tradeCancelRepository).reassignMember(member, dummy);
                verify(alertRepository).reassignSendMember(member, dummy);
                verify(noticeRepository).reassignMember(member, dummy);

                verify(memberRelatedKeywordRepository).deleteAllByMember(member);
                verify(suspendRepository).deleteAllByMember(member);
                verify(memberBlockRepository).deleteAllByBlockerOrBlocked(member);
                verify(alertReceiptRepository).deleteAllByMember(member);
                verify(memberDetailRepository).deleteByMember(member);

                verify(memberRepository).delete(member);
            }
        }

        @Nested
        @DisplayName("dummy 멤버가 존재하지 않을 때")
        class Context_with_no_dummy_member {

            @Test
            @DisplayName("NotFoundDummyMemberException을 던진다")
            void it_throws_not_found_dummy_member_exception() {
                MemberJpaEntity member = mock(MemberJpaEntity.class);

                when(memberRepository.findByNickname("__withdrawn__")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.delete(member))
                        .isInstanceOf(NotFoundDummyMemberException.class);
            }
        }

        @Nested
        @DisplayName("삭제 대상 멤버가 dummy 멤버 본인일 때")
        class Context_with_dummy_member_self_deletion {

            @Test
            @DisplayName("DummyMemberDeletionNotAllowedException을 던진다")
            void it_throws_dummy_member_deletion_not_allowed_exception() {
                MemberJpaEntity member = mock(MemberJpaEntity.class);
                MemberJpaEntity dummy = mock(MemberJpaEntity.class);

                when(member.getId()).thenReturn(99L);
                when(dummy.getId()).thenReturn(99L);
                when(memberRepository.findByNickname("__withdrawn__")).thenReturn(Optional.of(dummy));

                assertThatThrownBy(() -> service.delete(member))
                        .isInstanceOf(DummyMemberDeletionNotAllowedException.class);
            }
        }
    }
}