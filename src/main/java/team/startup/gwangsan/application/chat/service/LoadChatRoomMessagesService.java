package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.data.ChatMessageRow;
import team.startup.gwangsan.application.chat.data.ChatMessageView;
import team.startup.gwangsan.application.chat.data.ChatProductCard;
import team.startup.gwangsan.application.chat.data.ChatRoomMessages;
import team.startup.gwangsan.application.chat.data.ChatTradeCard;
import team.startup.gwangsan.application.chat.data.LoadChatRoomMessagesCommand;
import team.startup.gwangsan.application.chat.port.in.LoadChatRoomMessagesUseCase;
import team.startup.gwangsan.application.chat.port.out.ChatMessageQueryPort;
import team.startup.gwangsan.application.chat.port.out.ChatProductPort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.chat.model.MessageType;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoadChatRoomMessagesService implements LoadChatRoomMessagesUseCase {

    private final ChatRoomPort roomPort;
    private final ChatProductPort productPort;
    private final ChatMessageQueryPort messageQueryPort;

    @Override
    @Transactional
    public ChatRoomMessages execute(LoadChatRoomMessagesCommand command) {
        Long memberId = command.actor().memberId();
        ChatRoom room = roomPort.loadWithProduct(command.roomId()).orElseThrow(NotFoundChatRoomException::new);
        if (!room.isParticipant(memberId)) {
            throw new NotFoundChatRoomException();
        }

        ChatProductCard product = productCard(room, memberId);

        List<ChatMessageRow> rows = messageQueryPort.findPage(
                command.roomId(), command.lastCreatedAt(), command.lastMessageId(), command.limit());
        List<Long> imageMessageIds = rows.stream()
                .filter(row -> row.messageType() == MessageType.IMAGE)
                .map(ChatMessageRow::messageId)
                .toList();
        Map<Long, List<ChatImage>> images = imageMessageIds.isEmpty()
                ? Map.of()
                : messageQueryPort.findImages(imageMessageIds);

        List<ChatMessageView> messages = rows.stream()
                .map(row -> new ChatMessageView(
                        row.messageId(), row.roomId(), row.content(), row.messageType(),
                        row.createdAt(), row.editedAt(), images.getOrDefault(row.messageId(), List.of()),
                        row.senderNickname(), row.senderId(), row.checked(), row.senderId().equals(memberId)))
                .toList();

        // 응답을 만든 뒤, 이번 페이지의 최신 메시지까지 상대 메시지를 읽음 처리한다.
        if (!rows.isEmpty()) {
            messageQueryPort.markRead(command.roomId(), rows.getFirst().messageId(), memberId);
        }
        return new ChatRoomMessages(product, messages);
    }

    private ChatProductCard productCard(ChatRoom room, Long memberId) {
        ChatTradeCard card = productPort.loadTradeCard(room.productId(), room.buyerId(), room.sellerId());
        boolean isSeller = room.isSeller(memberId);
        ChatTradeCard.Reservation reservation = card.reservation();
        return new ChatProductCard(
                card.productId(),
                card.title(),
                card.images(),
                card.requestedAt(),
                isSeller,
                card.ownerId().equals(memberId),
                isSeller ? card.completableBySeller() : card.completableByBuyer(),
                card.completed(),
                card.reserved(),
                card.deleted(),
                reservation == null ? null : reservation.scheduledAt(),
                reservation == null ? null : reservation.reservedAt(),
                card.completedAt(),
                reservation == null ? null : reservation.placeName(),
                reservation == null ? null : reservation.address(),
                reservation == null ? null : reservation.latitude(),
                reservation == null ? null : reservation.longitude()
        );
    }
}
