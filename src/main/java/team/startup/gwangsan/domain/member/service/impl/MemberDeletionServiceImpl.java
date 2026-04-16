package team.startup.gwangsan.domain.member.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.alert.repository.AlertReceiptRepository;
import team.startup.gwangsan.domain.alert.repository.AlertRepository;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.member.service.MemberDeletionService;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.report.repository.ReportRepository;
import team.startup.gwangsan.domain.review.repository.ReviewRepository;
import team.startup.gwangsan.domain.suspend.repository.SuspendRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;

@Service
@RequiredArgsConstructor
public class MemberDeletionServiceImpl implements MemberDeletionService {

    private static final String WITHDRAWN_NICKNAME = "__withdrawn__";

    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final ProductRepository productRepository;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final AdminAlertRepository adminAlertRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final AlertRepository alertRepository;
    private final NoticeRepository noticeRepository;
    private final MemberRelatedKeywordRepository memberRelatedKeywordRepository;
    private final SuspendRepository suspendRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final AlertReceiptRepository alertReceiptRepository;

    @Override
    @Transactional
    public void delete(Member member) {
        Member dummy = memberRepository.findByNickname(WITHDRAWN_NICKNAME)
                .orElseThrow(NotFoundMemberException::new);

        reassignReferences(member, dummy);
        deletePersonalData(member);
        memberRepository.delete(member);
    }

    private void reassignReferences(Member member, Member dummy) {
        memberRepository.reassignRecommender(member, dummy);
        productRepository.reassignMember(member, dummy);
        tradeCompleteRepository.reassignBuyer(member, dummy);
        tradeCompleteRepository.reassignSeller(member, dummy);
        chatRoomRepository.reassignBuyer(member, dummy);
        chatRoomRepository.reassignSeller(member, dummy);
        chatMessageRepository.reassignSender(member, dummy);
        reviewRepository.reassignReviewer(member, dummy);
        reviewRepository.reassignReviewed(member, dummy);
        reportRepository.reassignReporter(member, dummy);
        reportRepository.reassignReported(member, dummy);
        adminAlertRepository.reassignOtherMember(member, dummy);
        adminAlertRepository.reassignRequester(member, dummy);
        tradeCancelRepository.reassignMember(member, dummy);
        alertRepository.reassignSendMember(member, dummy);
        noticeRepository.reassignMember(member, dummy);
    }

    private void deletePersonalData(Member member) {
        memberRelatedKeywordRepository.deleteAllByMember(member);
        suspendRepository.deleteAllByMember(member);
        memberBlockRepository.deleteAllByBlockerOrBlocked(member);
        alertReceiptRepository.deleteAllByMember(member);
        memberDetailRepository.deleteByMember(member);
    }
}