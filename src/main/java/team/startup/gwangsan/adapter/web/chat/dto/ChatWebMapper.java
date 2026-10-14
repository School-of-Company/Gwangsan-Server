package team.startup.gwangsan.adapter.web.chat.dto;

import team.startup.gwangsan.application.chat.data.ChatMessageView;
import team.startup.gwangsan.application.chat.data.ChatProductCard;
import team.startup.gwangsan.application.chat.data.ChatRoomListRow;
import team.startup.gwangsan.application.chat.data.ChatRoomMessages;
import team.startup.gwangsan.application.chat.data.ChatRoomProduct;
import team.startup.gwangsan.application.chat.data.ChatRoomSummary;
import team.startup.gwangsan.application.chat.data.UpdatedChatMessage;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.util.List;

/** application 결과 → 기존 HTTP 응답 DTO. JSON 이름·순서는 DTO record 가 고정한다. */
public final class ChatWebMapper {

    private ChatWebMapper() {
    }

    public static GetChatMessagesResponse toResponse(ChatRoomMessages result) {
        return new GetChatMessagesResponse(toResponse(result.product()),
                result.messages().stream().map(ChatWebMapper::toResponse).toList());
    }

    public static List<GetRoomsResponse> toRoomsResponse(List<ChatRoomSummary> summaries) {
        return summaries.stream().map(ChatWebMapper::toResponse).toList();
    }

    public static UpdateChatMessageResponse toResponse(UpdatedChatMessage updated) {
        return new UpdateChatMessageResponse(updated.messageId(), updated.roomId(), updated.content(), updated.editedAt());
    }

    public static ValidateChatSendableResponse toSendableResponse(List<ChatImage> images) {
        return new ValidateChatSendableResponse(images(images));
    }

    public static GetRoomProductDto toResponse(ChatRoomProduct product) {
        return new GetRoomProductDto(product.productId(), product.title(), product.isCompleted(),
                product.isReserved(), product.isDeleted(), images(product.images()));
    }

    public static List<ChatImage> toImages(List<GetImageResponse> images) {
        return images.stream().map(image -> new ChatImage(image.imageId(), image.imageUrl())).toList();
    }

    private static GetRoomsResponse toResponse(ChatRoomSummary summary) {
        ChatRoomListRow room = summary.room();
        return new GetRoomsResponse(
                room.roomId(),
                new GetRoomMemberResponse(room.opponentId(), room.opponentNickname()),
                room.messageId(),
                room.lastMessage(),
                room.lastMessageType(),
                room.lastMessageTime(),
                room.unreadMessageCount(),
                toResponse(summary.product()));
    }

    private static GetChatProductDto toResponse(ChatProductCard card) {
        return new GetChatProductDto(card.id(), card.title(), images(card.images()), card.createdAt(),
                card.isSeller(), card.isAuthor(), card.isCompletable(), card.isCompleted(), card.isReserved(),
                card.isDeleted(), card.reservationScheduledAt(), card.reservedAt(), card.completedAt(),
                card.reservationPlaceName(), card.reservationAddress(), card.reservationLatitude(),
                card.reservationLongitude());
    }

    private static GetChatMessageDto toResponse(ChatMessageView message) {
        return new GetChatMessageDto(message.messageId(), message.roomId(), message.content(), message.messageType(),
                message.createdAt(), message.editedAt(), images(message.images()), message.senderNickname(),
                message.senderId(), message.checked(), message.isMine());
    }

    private static List<GetImageResponse> images(List<ChatImage> images) {
        return images.stream().map(image -> new GetImageResponse(image.imageId(), image.imageUrl())).toList();
    }
}
