package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.SaveChatMessageResponse;
import team.startup.gwangsan.domain.chat.repository.ChatMessageImageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.SaveChatMessageService;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.global.event.SendNotificationEvent;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    @Transactional
    public SaveChatMessageResponse execute(Long messageId, Long roomId, String content, List<Long> imageIds, MessageType messageType, Long senderId, LocalDateTime createdAt) {
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

        chatMessageRepository.save(chatMessage);

        List<ChatMessageImage> chatMessageImages = List.of();

        if (messageType == MessageType.IMAGE && imageIds != null && !imageIds.isEmpty()) {
            List<Image> images = imageRepository.findAllById(imageIds);
            chatMessageImages = mapToChatMessageImages(images, chatMessage);
            chatMessageImageRepository.saveAll(chatMessageImages);
        }

        Member otherMember = member.getId().equals(chatRoom.getBuyer().getId()) ? chatRoom.getSeller() : chatRoom.getBuyer();

        deviceTokenRepository.findByUserId(otherMember.getId())
                .ifPresent(token -> applicationEventPublisher.publishEvent(
                        new SendNotificationEvent(List.of(token), NotificationType.CHATTING, roomId)
                ));

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

    private List<ChatMessageImage> mapToChatMessageImages(List<Image> images, ChatMessage chatMessage) {
        return images.stream()
                .map(image -> ChatMessageImage.builder()
                        .image(image)
                        .chatMessage(chatMessage)
                        .build())
                .toList();
    }
}
