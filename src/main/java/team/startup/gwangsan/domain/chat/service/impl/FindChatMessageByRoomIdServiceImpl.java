package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.ChatMessageImage;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetChatMessageResponse;
import team.startup.gwangsan.domain.chat.repository.ChatMessageImageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.FindChatMessageByRoomIdService;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindChatMessageByRoomIdServiceImpl implements FindChatMessageByRoomIdService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageImageRepository chatMessageImageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetChatMessageResponse> execute(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit) {
        Member member = memberUtil.getCurrentMember();

        ChatRoom chatRoom = chatRoomRepository.findChatRoomByRoomId(roomId)
                .orElseThrow(NotFoundChatRoomException::new);

        Member otherMember = chatRoom.getMember1() == member ? chatRoom.getMember2() : chatRoom.getMember1();

        Member member1 = member.getId() < otherMember.getId() ? member : otherMember;
        Member member2 = member.getId() < otherMember.getId() ? otherMember : member;

        List<ChatRoom> chatRooms = chatRoomRepository.findAllByMember1AndMember2(member1, member2);

        List<Long> chatRoomIds = chatRooms.stream().map(ChatRoom::getId).toList();

        List<ChatMessage> messages = chatMessageRepository.findChatMessageByRoomIdsWithCursorPaging(chatRoomIds, lastCreatedAt, lastMessageId, limit);
        List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();

        List<ChatMessageImage> images = chatMessageImageRepository.findAllByChatMessageIdIn(messageIds);

        Map<Long, List<GetImageResponse>> imageMap = images.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getChatMessage().getId(),
                        Collectors.mapping(
                                img -> new GetImageResponse(img.getImage().getId(), img.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        return messages.stream()
                .map(message -> new GetChatMessageResponse(
                            message.getId(),
                            message.getRoom().getId(),
                            message.getContent(),
                            message.getMessageType(),
                            message.getCreatedAt(),
                            imageMap.getOrDefault(message.getId(), List.of()),
                            message.getSender().getNickname(),
                            message.getSender().getId(),
                            message.getChecked(),
                            message.getSender().equals(member)
                ))
                .toList();
    }
}
