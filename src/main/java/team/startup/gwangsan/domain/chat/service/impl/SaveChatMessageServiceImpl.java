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
import team.startup.gwangsan.domain.notification.entity.DeviceToken;
import team.startup.gwangsan.domain.notification.entity.constant.NotificationType;
import team.startup.gwangsan.domain.notification.repository.DeviceTokenRepository;
import team.startup.gwangsan.global.event.SendNotificationEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaveChatMessageServiceImpl implements SaveChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final ImageRepository imageRepository;
    private final ChatMessageImageRepository chatMessageImageRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional
    public SaveChatMessageResponse execute(Long roomId, String content, List<Long> imageIds, MessageType messageType) {
        Member member = memberUtil.getCurrentMember();
        ChatRoom chatRoom = chatRoomRepository.findChatRoomByRoomId(roomId)
                .orElseThrow(NotFoundChatRoomException::new);

        ChatMessage chatMessage = ChatMessage.builder()
                .content(content)
                .sender(member)
                .room(chatRoom)
                .messageType(messageType)
                .checked(false)
                .build();

        chatMessageRepository.save(chatMessage);

        List<ChatMessageImage> chatMessageImages = List.of();

        if (messageType == MessageType.IMAGE && imageIds != null && !imageIds.isEmpty()) {
            List<Image> images = imageRepository.findAllById(imageIds);
            chatMessageImages = mapToChatMessageImages(images, chatMessage);
            chatMessageImageRepository.saveAll(chatMessageImages);
        }

        Member otherMember = member.getId().equals(chatRoom.getMember1().getId()) ? chatRoom.getMember2() : chatRoom.getMember1();

        Optional<DeviceToken> optionalToken = deviceTokenRepository.findByUserId(otherMember.getId());

        optionalToken.ifPresent(token -> {
            List<String> deviceTokens = List.of(token.getDeviceToken());

            applicationEventPublisher.publishEvent(new SendNotificationEvent(
                    deviceTokens,
                    NotificationType.CHATTING
            ));
        });

        return new SaveChatMessageResponse(
                chatMessage.getId(),
                chatMessageImages.stream()
                        .map(mi -> new GetImageResponse(
                                mi.getId(),
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
