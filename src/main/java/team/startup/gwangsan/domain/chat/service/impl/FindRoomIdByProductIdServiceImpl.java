package team.startup.gwangsan.domain.chat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.GetRoomIdResponse;
import team.startup.gwangsan.domain.chat.repository.ChatRoomRepository;
import team.startup.gwangsan.domain.chat.service.FindRoomIdByProductIdService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class FindRoomIdByProductIdServiceImpl implements FindRoomIdByProductIdService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public GetRoomIdResponse execute(Long productId) {
        Member member = memberUtil.getCurrentMember();
        ChatRoom chatRoom = chatRoomRepository.findByProductIdAndMember(productId, member)
                .orElseThrow(NotFoundChatRoomException::new);

        return new GetRoomIdResponse(chatRoom.getId());
    }
}
