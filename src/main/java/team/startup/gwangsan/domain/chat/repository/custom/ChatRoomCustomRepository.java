package team.startup.gwangsan.domain.chat.repository.custom;

import team.startup.gwangsan.domain.chat.entity.ChatRoom;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomsDto;
import team.startup.gwangsan.domain.member.entity.Member;

import java.util.List;
import java.util.Optional;

public interface ChatRoomCustomRepository {
    Optional<ChatRoom> findChatRoomByRoomId(Long roomId);

    Optional<ChatRoom> findByProductIdAndMember(Long productId, Member member);

    List<GetRoomsDto> findRoomsByMemberId(Long memberId);
}
