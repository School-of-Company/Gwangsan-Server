package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.GetChatMessageDto;
import team.startup.gwangsan.domain.chat.presentation.dto.GetChatProductDto;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetChatMessagesResponse;
import team.startup.gwangsan.domain.chat.repository.ChatMessageImageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.FindChatMessageByRoomIdService;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.ProductReservation;
import team.startup.gwangsan.domain.post.entity.constant.ReservationStatus;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductReservationRepository;
import team.startup.gwangsan.domain.trade.service.TradeStateReader;
import team.startup.gwangsan.domain.trade.service.TradeStateSnapshot;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindChatMessageByRoomIdServiceImpl implements FindChatMessageByRoomIdService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageImageRepository chatMessageImageRepository;
    private final ProductImageRepository productImageRepository;
    private final TradeStateReader tradeStateReader;
    private final ProductReservationRepository productReservationRepository;

    @Override
    @Transactional
    public GetChatMessagesResponse execute(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit) {
        Long memberId = memberUtil.getCurrentMember().getId();

        ChatRoom chatRoom = chatRoomRepository.findByRoomIdWithSellerAndProduct(roomId)
                .orElseThrow(NotFoundChatRoomException::new);

        if (!memberId.equals(chatRoom.getSeller().getId()) && !memberId.equals(chatRoom.getBuyer().getId())) {
            throw new NotFoundChatRoomException();
        }

        Product product = chatRoom.getProduct();

        List<ProductImage> productImages = productImageRepository.findAllByProductId(product.getId());

        List<GetImageResponse> imageResponses = productImages.stream()
                .map(pi -> new GetImageResponse(
                        pi.getImage().getId(),
                        pi.getImage().getImageUrl()))
                .toList();

        boolean isSeller = memberId.equals(chatRoom.getSeller().getId());

        // 거래 상태 변경 이벤트와 같은 값을 쓰기 위해 조회도 이 스냅샷을 거친다.
        TradeStateSnapshot tradeState = tradeStateReader.read(product, chatRoom.getBuyer(), chatRoom.getSeller());
        boolean isReserved = tradeState.reserved();

        Optional<ProductReservation> reservation = isReserved
                ? productReservationRepository.findByProductAndStatus(product, ReservationStatus.PENDING)
                : Optional.empty();

        GetChatProductDto productDto = new GetChatProductDto(
                product.getId(),
                product.getTitle(),
                imageResponses,
                tradeState.requestedAt(),
                isSeller,
                tradeState.isCompletableFor(isSeller),
                tradeState.completed(),
                isReserved,
                reservation.map(ProductReservation::getScheduledAt).orElse(null),
                reservation.map(ProductReservation::getPlaceName).orElse(null),
                reservation.map(ProductReservation::getAddress).orElse(null),
                reservation.map(ProductReservation::getLatitude).orElse(null),
                reservation.map(ProductReservation::getLongitude).orElse(null)
        );

        List<ChatMessage> messages = chatMessageRepository.findChatMessageByRoomIdWithCursorPaging(roomId, lastCreatedAt, lastMessageId, limit);
        List<Long> imageMessageIds = messages.stream()
                .filter(message -> message.getMessageType() == MessageType.IMAGE)
                .map(ChatMessage::getId)
                .toList();

        Map<Long, List<GetImageResponse>> imageMap = imageMessageIds.isEmpty()
                ? Map.of()
                : chatMessageImageRepository.findAllByChatMessageIdIn(imageMessageIds)
                .stream()
                .collect(Collectors.groupingBy(
                        img -> img.getChatMessage().getId(),
                        Collectors.mapping(
                                img -> new GetImageResponse(img.getImage().getId(), img.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        List<GetChatMessageDto> chatMessageDtos = messages.stream()
                .map(message -> new GetChatMessageDto(
                        message.getId(),
                        message.getRoom().getId(),
                        message.getContent(),
                        message.getMessageType(),
                        message.getCreatedAt(),
                        imageMap.getOrDefault(message.getId(), List.of()),
                        message.getSender().getNickname(),
                        message.getSender().getId(),
                        message.getChecked(),
                        message.getSender().getId().equals(memberId)
                ))
                .toList();

        if (!messages.isEmpty()) {
            Long latestMessageId = messages.get(0).getId();
            chatMessageRepository.readMessage(roomId, latestMessageId, memberId);
        }

        return new GetChatMessagesResponse(productDto, chatMessageDtos);
    }
}
