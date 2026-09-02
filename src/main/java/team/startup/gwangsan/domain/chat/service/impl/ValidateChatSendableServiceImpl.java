package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.ValidateChatSendableService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.BlockValidator;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class ValidateChatSendableServiceImpl implements ValidateChatSendableService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;
    private final BlockValidator blockValidator;

    @Override
    @Transactional(readOnly = true)
    public void execute(Long roomId) {
        Member member = memberUtil.getCurrentMember();
        ChatRoom chatRoom = chatRoomRepository.findChatRoomByRoomId(roomId)
                .orElseThrow(NotFoundChatRoomException::new);

        // 참여자가 아니면 방의 존재 자체를 숨긴다 (조회 API 와 동일 정책).
        if (!chatRoom.isParticipant(member)) {
            throw new NotFoundChatRoomException();
        }

        blockValidator.validate(member, chatRoom.getOtherMember(member));
    }
}
