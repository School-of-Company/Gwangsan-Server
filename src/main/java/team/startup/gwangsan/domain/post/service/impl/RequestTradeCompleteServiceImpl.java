package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
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
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.post.exception.*;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.trade.exception.*;
import team.startup.gwangsan.domain.trade.exception.CannotSelectSelfException;
import team.startup.gwangsan.domain.trade.exception.SellerNotTradeCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteRequestException;
import team.startup.gwangsan.domain.trade.exception.TradeCompleteWithoutChattingException;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.post.service.RequestTradeCompleteService;
import team.startup.gwangsan.global.aop.CheckBlocked;
import team.startup.gwangsan.global.event.CreateAlertEvent;
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
    private final ProductReservationRepository productReservationRepository;

    @Override
    @Transactional
    @CheckBlocked(param = "otherMemberId")
    public void execute(Long productId, Long otherMemberId) {
        String phoneNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        MemberDetail memberDetail = memberDetailRepository.findByPhoneNumberWithMember(phoneNumber);
        Member member = memberDetail.getMember();

        validateNotSelfTrade(member.getId(), otherMemberId);

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(NotFoundProductException::new);
        validateProductStatus(product);

        MemberDetail otherMemberDetail = memberDetailRepository.findByMemberIdWithMember(otherMemberId);

        boolean isBuyer = isBuyer(product, member);
        MemberDetail buyerDetail  = isBuyer ? memberDetail    : otherMemberDetail;
        MemberDetail sellerDetail = isBuyer ? otherMemberDetail: memberDetail;

        ProductReservation reservation =
                validateReservationParticipant(product, buyerDetail.getMember(), sellerDetail.getMember());

        ChatRoom chatRoom = findChatRoom(product, buyerDetail.getMember(), sellerDetail.getMember());
        validateChatExists(chatRoom, member.getId());

        if (isBuyer) {
            handleBuyerTradeCompletion(product, buyerDetail, sellerDetail, reservation);
        } else {
            handleSellerTradeCompletion(product, sellerDetail.getMember(), buyerDetail.getMember());
        }

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

    private ProductReservation validateReservationParticipant(Product product, Member buyer, Member seller) {
        if (product.getStatus() != ProductStatus.RESERVATION) {
            return null;
        }

        ProductReservation reservation = productReservationRepository
                .findByProductAndStatus(product, ReservationStatus.PENDING)
                .orElseThrow(ReservationParticipantOnlyException::new);

        Member reserver = reservation.getReserver();

        if (!reserver.equals(buyer) && !reserver.equals(seller)) {
            throw new ReservationParticipantOnlyException();
        }

        return reservation;
    }

    private void handleBuyerTradeCompletion(Product product, MemberDetail buyerDetail, MemberDetail sellerDetail, ProductReservation reservation) {
        TradeComplete pending = tradeCompleteRepository
                .findByProductAndBuyerAndSellerAndStatus(
                        product, buyerDetail.getMember(), sellerDetail.getMember(), TradeStatus.PENDING)
                .orElseThrow(SellerNotTradeCompleteException::new);

        product.updateStatus(ProductStatus.COMPLETED);
        pending.updateStatus(TradeStatus.COMPLETED);
        pending.updateCompletedAt();

        buyerDetail.minusGwangsan(product.getGwangsan());
        sellerDetail.plusGwangsan(product.getGwangsan());

        if (reservation != null) {
            reservation.complete();
        }

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

        newTradeComplete = tradeCompleteRepository.save(newTradeComplete);

        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                newTradeComplete.getId(),
                newTradeComplete.getBuyer().getId(),
                AlertType.OTHER_MEMBER_TRADE_COMPLETE
        ));
    }
}
