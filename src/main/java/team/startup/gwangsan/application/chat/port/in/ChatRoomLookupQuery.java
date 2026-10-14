package team.startup.gwangsan.application.chat.port.in;

import team.startup.gwangsan.application.chat.data.ChatRoomSnapshot;

import java.util.Optional;

/** 다른 업무(거래·예약 등)가 채팅방 존재와 대화 여부를 확인하는 공개 조회. */
public interface ChatRoomLookupQuery {

    Optional<ChatRoomSnapshot> byRoomId(Long roomId);

    Optional<ChatRoomSnapshot> byProductAndParticipants(Long productId, Long buyerId, Long sellerId);

    /** 회원이 구매자 또는 판매자인 방 중 첫 번째. */
    Optional<ChatRoomSnapshot> byProductAndMember(Long productId, Long memberId);

    /** 삭제되지 않은 메시지를 보낸 적이 있는지. */
    boolean hasSentMessage(Long roomId, Long memberId);
}
