package team.startup.gwangsan.adapter.persistence.chat;

import team.startup.gwangsan.application.chat.data.ChatRoomListRow;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.util.List;
import java.util.Optional;

public interface ChatRoomCustomRepository {

    Optional<ChatRoomJpaEntity> findChatRoomByRoomId(Long roomId);

    Optional<ChatRoomJpaEntity> findByProductIdAndMember(Long productId, MemberJpaEntity member);

    List<ChatRoomListRow> findRoomsByMemberId(Long memberId);

    Optional<ChatRoomJpaEntity> findByRoomIdWithSellerAndProduct(Long roomId);
}
