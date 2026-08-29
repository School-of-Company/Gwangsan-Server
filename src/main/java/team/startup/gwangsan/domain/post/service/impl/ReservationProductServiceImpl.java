package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ProductAlreadyReservationException;
import team.startup.gwangsan.domain.post.exception.ProductNotOngoingException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.post.service.ReservationProductService;
import team.startup.gwangsan.domain.trade.service.TradeStateReader;
import team.startup.gwangsan.domain.trade.service.TradeStateSnapshot;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationProductServiceImpl implements ReservationProductService {

    private final ProductRepository productRepository;
    private final ProductReservationRepository productReservationRepository;
    private final MemberUtil memberUtil;
    private final ChatRoomRepository chatRoomRepository;
    private final TradeStateReader tradeStateReader;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void execute(Long productId, Long roomId, LocalDateTime scheduledAt, String placeName, String address, Double latitude, Double longitude) {
        Member member = memberUtil.getCurrentMember();

        Product product = productRepository.findActiveById(productId)
                .orElseThrow(NotFoundProductException::new);

        if (!product.getMember().getId().equals(member.getId())) {
            throw new ForbiddenProductException();
        }

        if (product.getStatus() == ProductStatus.RESERVATION) {
            throw new ProductAlreadyReservationException();
        }

        if (product.getStatus() != ProductStatus.ONGOING) {
            throw new ProductNotOngoingException();
        }

        ChatRoom chatRoom = chatRoomRepository.findChatRoomByRoomId(roomId)
                .orElseThrow(NotFoundChatRoomException::new);

        if (!chatRoom.getProduct().getId().equals(productId)) {
            throw new NotFoundChatRoomException();
        }

        Member reserver = chatRoom.getBuyer().getId().equals(member.getId())
                ? chatRoom.getSeller()
                : chatRoom.getBuyer();

        productReservationRepository.save(
                ProductReservation.builder()
                        .product(product)
                        .reserver(reserver)
                        .status(ReservationStatus.PENDING)
                        .scheduledAt(scheduledAt)
                        .placeName(placeName)
                        .address(address)
                        .latitude(latitude)
                        .longitude(longitude)
                        .build()
        );

        product.updateStatus(ProductStatus.RESERVATION);

        TradeStateSnapshot tradeState = tradeStateReader.read(product, chatRoom.getBuyer(), chatRoom.getSeller());
        applicationEventPublisher.publishEvent(new TradeStatusChangedEvent(
                chatRoom.getId(),
                productId,
                tradeState.completed(),
                tradeState.reserved(),
                tradeState.requestedBySeller(),
                tradeState.requestedAt()
        ));
    }
}
