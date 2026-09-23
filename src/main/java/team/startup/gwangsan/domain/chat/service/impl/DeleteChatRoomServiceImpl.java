package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.DeleteChatRoomService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeleteChatRoomServiceImpl implements DeleteChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long roomId) {
        Member member = memberUtil.getCurrentMember();

        ChatRoom chatRoom = chatRoomRepository.findChatRoomByRoomId(roomId)
                .orElseThrow(NotFoundChatRoomException::new);

        if (!chatRoom.isParticipant(member)) {
            throw new NotFoundChatRoomException();
        }

        chatRoom.hideFor(member, LocalDateTime.now());
    }
}
