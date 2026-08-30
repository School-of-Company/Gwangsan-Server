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
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteException;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteRequestException;
import team.startup.gwangsan.domain.trade.exception.TradeCompleteWithoutChattingException;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.post.service.RequestTradeCompleteService;
import team.startup.gwangsan.global.aop.CheckBlocked;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.event.SendNotificationEvent;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        Optional<TradeComplete> pending = tradeCompleteRepository.findByProductAndBuyerAndSellerAndStatus(
                product, buyerDetail.getMember(), sellerDetail.getMember(), TradeStatus.PENDING);

        if (pending.isPresent()) {
            confirmTradeCompletion(pending.get(), isBuyer, chatRoom, product, buyerDetail, sellerDetail, otherMemberDetail.getMember(), reservation);
        } else {
            requestTradeCompletion(chatRoom, product, isBuyer, buyerDetail.getMember(), sellerDetail.getMember(), otherMemberDetail.getMember());
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

    /**
     * PENDING 요청을 만든 쪽(requestedBySeller)과 현재 호출자(isBuyer)의 역할이 같다면,
     * 본인이 만든 요청을 스스로 확정하려는 것이므로 확정을 막는다.
     */
    private boolean isCallerTheRequester(boolean isBuyer, TradeComplete pending) {
        return isBuyer != pending.isRequestedBySeller();
    }

    private void confirmTradeCompletion(TradeComplete pending, boolean isBuyer, ChatRoom chatRoom, Product product,
                                         MemberDetail buyerDetail, MemberDetail sellerDetail, Member requester,
                                         ProductReservation reservation) {
        if (isCallerTheRequester(isBuyer, pending)) {
            throw new TradeAlreadyCompleteRequestException();
        }

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
            deviceTokens.addAll(deviceTokenRepository.findAllByUserId(memberId));
        }

        tradeCompleteRepository.deleteByProductAndStatus(product, TradeStatus.PENDING);

        applicationEventPublisher.publishEvent(new SendNotificationEvent(
                deviceTokens,
                NotificationType.TRADE_COMPLETE,
                product.getId()
        ));

        // 확정 후에는 대기 중인 요청이 없으므로 requestedBySeller 는 null 이고,
        // requestedAt 은 방금 완료된 요청의 생성 시각을 유지한다. 조회 응답과 같은 값이다.
        applicationEventPublisher.publishEvent(new TradeStatusChangedEvent(
                chatRoom.getId(),
                product.getId(),
                true,
                false,
                null,
                pending.getCreatedAt()
        ));
    }

    private void requestTradeCompletion(ChatRoom chatRoom, Product product, boolean isBuyer, Member buyer, Member seller, Member requestTarget) {
        TradeComplete newTradeComplete = TradeComplete.builder()
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .status(TradeStatus.PENDING)
                .requestedBySeller(!isBuyer)
                .build();

        newTradeComplete = tradeCompleteRepository.save(newTradeComplete);

        applicationEventPublisher.publishEvent(new CreateAlertEvent(
                newTradeComplete.getId(),
                requestTarget.getId(),
                AlertType.OTHER_MEMBER_TRADE_COMPLETE
        ));

        // save() 로 @CreatedDate 가 채워지므로 조회 응답이 나중에 줄 값과 동일하다.
        applicationEventPublisher.publishEvent(new TradeStatusChangedEvent(
                chatRoom.getId(),
                product.getId(),
                false,
                product.getStatus() == ProductStatus.RESERVATION,
                newTradeComplete.isRequestedBySeller(),
                newTradeComplete.getCreatedAt()
        ));
    }
}
