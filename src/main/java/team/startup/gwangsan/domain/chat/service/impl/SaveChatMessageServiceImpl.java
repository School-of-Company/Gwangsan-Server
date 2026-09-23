package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.ChatMessageIdConflictException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatMessageException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.SaveChatMessageResponse;
import team.startup.gwangsan.domain.chat.repository.ChatMessageImageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.repository.custom.ChatMessageCustomRepository.StoredMessage;
import team.startup.gwangsan.domain.chat.service.SaveChatMessageService;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.global.event.SendNotificationEvent;
import team.startup.gwangsan.global.util.BlockValidator;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaveChatMessageServiceImpl implements SaveChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final ChatMessageImageRepository chatMessageImageRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DeviceTokenRepository deviceTokenRepository;
    private final BlockValidator blockValidator;

    @Override
    @Transactional
    public SaveChatMessageResponse execute(Long messageId, Long roomId, String content, List<Long> imageIds, MessageType messageType, Long senderId, LocalDateTime createdAt) {
        if (chatMessageRepository.existsById(messageId)) {
            return replay(messageId, roomId, content, imageIds, messageType, senderId, createdAt);
        }

        Member member = memberRepository.findById(senderId).orElseThrow(NotFoundMemberException::new);
        ChatRoom chatRoom = chatRoomRepository.findChatRoomByRoomId(roomId)
                .orElseThrow(NotFoundChatRoomException::new);

        ChatMessage chatMessage = ChatMessage.builder()
                .id(messageId)
                .content(content)
                .sender(member)
                .room(chatRoom)
                .messageType(messageType)
                .checked(false)
                .createdAt(createdAt)
                .build();

        if (!chatMessageRepository.insertIfAbsent(chatMessage)) {
            return replay(messageId, roomId, content, imageIds, messageType, senderId, createdAt);
        }

        Member otherMember = chatRoom.getOtherMember(member);
        blockValidator.validate(member, otherMember);
        boolean recipientHidden = chatRoom.isHiddenFor(otherMember);

        List<ChatMessageImage> chatMessageImages = List.of();

        if (messageType == MessageType.IMAGE && imageIds != null && !imageIds.isEmpty()) {
            List<Image> images = imageRepository.findAllById(imageIds);
            chatMessageImages = mapToChatMessageImages(images, chatMessageRepository.getReferenceById(messageId));
            chatMessageImageRepository.saveAll(chatMessageImages);
        }

        if (!recipientHidden) {
            List<DeviceToken> deviceTokens = deviceTokenRepository.findAllByUserId(otherMember.getId());
            if (!deviceTokens.isEmpty()) {
                applicationEventPublisher.publishEvent(
                        new SendNotificationEvent(deviceTokens, NotificationType.CHATTING, roomId)
                );
            }
        }

        return new SaveChatMessageResponse(
                chatMessage.getId(),
                chatMessageImages.stream()
                        .map(mi -> new GetImageResponse(
                                mi.getImage().getId(),
                                mi.getImage().getImageUrl()
                        ))
                        .toList(),
                chatMessage.getCreatedAt(),
                member.getId(),
                chatMessage.getChecked()
        );
    }

    private SaveChatMessageResponse replay(Long messageId, Long roomId, String content, List<Long> imageIds,
                                           MessageType messageType, Long senderId, LocalDateTime createdAt) {
        StoredMessage stored = chatMessageRepository.findStoredMessage(messageId)
                .orElseThrow(NotFoundChatMessageException::new);
        if (!Objects.equals(stored.roomId(), roomId)
                || !Objects.equals(stored.senderId(), senderId)
                || !Objects.equals(stored.content(), content)
                || stored.messageType() != messageType
                || !stored.createdAt().truncatedTo(ChronoUnit.MILLIS).equals(createdAt.truncatedTo(ChronoUnit.MILLIS))) {
            throw new ChatMessageIdConflictException(messageId);
        }
        if (messageType == MessageType.IMAGE) {
            Set<Long> requestedImages = imageIds == null ? Set.of() : chatMessageRepository.findExistingImageIds(imageIds);
            Set<Long> storedImages = stored.images().stream().map(GetImageResponse::imageId).collect(Collectors.toSet());
            if (!storedImages.equals(requestedImages)) {
                throw new ChatMessageIdConflictException(messageId);
            }
        }
        return new SaveChatMessageResponse(stored.messageId(), stored.images(), stored.createdAt(),
                stored.senderId(), stored.checked());
    }

    private List<ChatMessageImage> mapToChatMessageImages(List<Image> images, ChatMessage chatMessage) {
        return images.stream()
                .map(image -> ChatMessageImage.builder()
                        .image(image)
                        .chatMessage(chatMessage)
                        .build())
                .toList();
    }
}
