package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.TradeComplete;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.post.exception.*;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.post.service.RequestTradeCompleteService;
import team.startup.gwangsan.global.event.SendNotificationEvent;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestTradeCompleteServiceImpl implements RequestTradeCompleteService {

    private final ProductRepository productRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TradeCompleteRepository tradeCompleteRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void execute(Long productId, Long otherMemberId) {
        String phoneNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        MemberDetail memberDetail = memberDetailRepository.findByPhoneNumberWithMember(phoneNumber);
        Member member = memberDetail.getMember();

        validateNotSelfTrade(member.getId(), otherMemberId);

        Product product = productRepository.findById(productId)
                .orElseThrow(NotFoundProductException::new);
        validateProductStatus(product);

        MemberDetail otherMemberDetail = memberDetailRepository.findByMemberIdWithMember(otherMemberId);

        boolean isBuyer = isBuyer(product, member);
        MemberDetail buyerDetail  = isBuyer ? memberDetail    : otherMemberDetail;
        MemberDetail sellerDetail = isBuyer ? otherMemberDetail: memberDetail;

        ChatRoom chatRoom = findChatRoom(product, buyerDetail.getMember(), sellerDetail.getMember());
        validateChatExists(chatRoom, member.getId());

        if (isBuyer) handleBuyerTradeCompletion(product, buyerDetail, sellerDetail);
        else         handleSellerTradeCompletion(product, sellerDetail.getMember(), buyerDetail.getMember());

    }

    private void validateNotSelfTrade(Long memberId, Long otherMemberId) {
        if (memberId.equals(otherMemberId)) {
            throw new CannotSelectSelfException();
        }
    }

    private void validateProductStatus(Product product) {
        if (product.getStatus() == ProductStatus.COMPLETED) {
            throw new TradeAlreadyCompleteException();
        }
    }

    private ChatRoom findChatRoom(Product product, Member buyer, Member seller) {
        return chatRoomRepository.findByProductIdAndBuyerAndSeller(product.getId(), buyer, seller)
                .orElseThrow(NotFoundChatRoomException::new);
    }

    private void validateChatExists(ChatRoom room, Long memberId) {
        boolean hasChat = chatMessageRepository.existsByRoomAndSenderId(room, memberId);
        if (!hasChat) {
            throw new TradeCompleteWithoutChattingException();
        }
    }

    private boolean isBuyer(Product product, Member member) {
        if (product.getMode() == Mode.GIVER) {
            return !product.getMember().equals(member);
        } else {
            return product.getMember().equals(member);
        }
    }

    private void handleBuyerTradeCompletion(Product product, MemberDetail buyerDetail, MemberDetail sellerDetail) {
        TradeComplete pending = tradeCompleteRepository
                .findByProductAndBuyerAndSellerAndStatus(
                        product, buyerDetail.getMember(), sellerDetail.getMember(), TradeStatus.PENDING)
                .orElseThrow(SellerNotTradeCompleteException::new);

        product.updateStatus(ProductStatus.COMPLETED);
        pending.updateStatus(TradeStatus.COMPLETED);
        pending.updateCompletedAt();

        buyerDetail.minusGwangsan(product.getGwangsan());
        sellerDetail.plusGwangsan(product.getGwangsan());

        List<Long> memberIds = new ArrayList<>();

        memberIds.add(buyerDetail.getId());
        memberIds.add(sellerDetail.getId());

        List<DeviceToken> deviceTokens = new ArrayList<>();

        for (Long memberId : memberIds) {
            deviceTokenRepository.findByUserId(memberId)
                    .ifPresent(deviceTokens::add);
        }

        tradeCompleteRepository.deleteByProductAndStatus(product, TradeStatus.PENDING);

        applicationEventPublisher.publishEvent(new SendNotificationEvent(
                deviceTokens,
                NotificationType.TRADE_COMPLETE,
                product.getId()
        ));
    }

    private void handleSellerTradeCompletion(Product product, Member seller, Member buyer) {
        boolean existsTradeComplete = tradeCompleteRepository
                .existsByProductAndBuyerAndSellerAndStatus(product, buyer, seller, TradeStatus.PENDING);

        if (existsTradeComplete) {
            throw new TradeAlreadyCompleteRequestException();
        }
        TradeComplete newTradeComplete = TradeComplete.builder()
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .status(TradeStatus.PENDING)
                .build();

        tradeCompleteRepository.save(newTradeComplete);
    }
}
