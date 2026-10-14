package team.startup.gwangsan.application.chat.port.in;

import team.startup.gwangsan.domain.common.ActorContext;

public interface CreateChatRoomUseCase {
    /** 같은 상품·참여자 방이 있으면 실행자 숨김만 풀고 그 방 ID 를 돌려준다. */
    Long execute(ActorContext actor, Long productId);
}
