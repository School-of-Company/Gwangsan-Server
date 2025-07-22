package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.TradeComplete;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.exception.CannotCompleteTradeException;
import team.startup.gwangsan.domain.post.exception.CannotSelectSelfException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.post.service.RequestTradeCompleteService;
import team.startup.gwangsan.global.event.CreateAdminAlertEvent;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class RequestTradeCompleteServiceImpl implements RequestTradeCompleteService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberUtil memberUtil;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    @Transactional
    public void execute(Long productId, Long otherMemberId) {
        Member member = memberUtil.getCurrentMember();

        validateNotSelfTrade(member.getId(), otherMemberId);

        Product product = findProductById(productId);
        validateProductStatus(product);

        Member otherMember = findMemberById(otherMemberId);

        ChatRoom chatRoom = findChatRoom(productId, member, otherMember);
        //validateChatExists(chatRoom, otherMember.getId());
        validateNotAlreadyRequested(product, member, otherMember);

        TradeComplete tradeComplete = TradeComplete.builder()
                .product(product)
                .member(member)
                .otherMember(otherMember)
                .build();

        saveTradeComplete(tradeComplete);

        notifyIfMutualComplete(productId, member, otherMember, product.getMember());

        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                tradeComplete.getId(),
                otherMemberId,
                team.startup.gwangsan.domain.alert.entity.constant.AlertType.OTHER_MEMBER_TRADE_COMPLETE));
    }

    private void validateNotSelfTrade(Long memberId, Long otherMemberId) {
        if (memberId.equals(otherMemberId)) {
            throw new CannotSelectSelfException();
        }
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);
    }

    private void validateProductStatus(Product product) {
        if (product.getStatus() == ProductStatus.COMPLETED) {
            throw new CannotCompleteTradeException();
        }
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);
    }

    private ChatRoom findChatRoom(Long productId, Member member, Member otherMember) {
        Member member1 = member.getId() < otherMember.getId() ? member : otherMember;
        Member member2 = member.getId() < otherMember.getId() ? otherMember : member;

        return chatRoomRepository.findByProductIdAndMember1AndMember2(productId, member1, member2)
                .orElseThrow(CannotCompleteTradeException::new);
    }

    private void validateChatExists(ChatRoom room, Long otherMemberId) {
        boolean hasChat = chatMessageRepository.existsByRoomAndSenderId(room, otherMemberId);
        if (!hasChat) {
            throw new CannotCompleteTradeException();
        }
    }

    private void validateNotAlreadyRequested(Product product, Member member, Member otherMember) {
        boolean alreadyRequested = tradeCompleteRepository.existsByProductAndMemberAndOtherMember(
                product, member, otherMember
        );
        if (alreadyRequested) {
            throw new CannotCompleteTradeException();
        }
    }

    private void saveTradeComplete(TradeComplete tradeComplete) {
        tradeCompleteRepository.save(tradeComplete);
    }

    private void notifyIfMutualComplete(Long productId, Member member, Member otherMember, Member productMember) {
        long count = tradeCompleteRepository.countMutualTradeComplete(
                productId, member.getId(), otherMember.getId());

        if (count == 2) {
            Member sender;
            Member receiver;

            if (member.getId().equals(productMember.getId())) {
                sender = member;
                receiver = otherMember;
            } else if (otherMember.getId().equals(productMember.getId())) {
                sender = otherMember;
                receiver = member;
            } else {
                throw new CannotCompleteTradeException();
            }

            applicationEventPublisher.publishEvent(new CreateAdminAlertEvent(
                    AlertType.TRADE_COMPLETE,
                    productId,
                    sender.getId(),
                    receiver.getId()
            ));
        }
    }
}
