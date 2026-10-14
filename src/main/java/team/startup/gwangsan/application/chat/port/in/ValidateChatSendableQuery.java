package team.startup.gwangsan.application.chat.port.in;

import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;
import java.util.Optional;

/** 채팅 서버가 소켓 전송 전에 호출한다. imageIds 가 null 이면 이미지 검증 없이 빈 결과. */
public interface ValidateChatSendableQuery {
    Optional<List<ChatImage>> query(ActorContext actor, Long roomId, List<String> imageIds);
}
