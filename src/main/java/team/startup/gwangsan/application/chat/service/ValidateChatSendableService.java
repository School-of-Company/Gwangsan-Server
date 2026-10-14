package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.port.in.ValidateChatSendableQuery;
import team.startup.gwangsan.application.chat.port.out.ChatBlockPort;
import team.startup.gwangsan.application.chat.port.out.ChatImagePort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.chat.policy.ChatImageIdPolicy;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ValidateChatSendableService implements ValidateChatSendableQuery {

    private final ChatRoomPort roomPort;
    private final ChatBlockPort blockPort;
    private final ChatImagePort imagePort;

    /** 검사 순서: 방(404) → 참여자(404) → 차단(403) → 이미지 ID 형식(400) → 이미지 존재(404). */
    @Override
    @Transactional(readOnly = true)
    public Optional<List<ChatImage>> query(ActorContext actor, Long roomId, List<String> imageIds) {
        Long memberId = actor.memberId();
        ChatRoom room = roomPort.load(roomId).orElseThrow(NotFoundChatRoomException::new);
        // 참여자가 아니면 방의 존재 자체를 숨긴다 (조회 API 와 동일 정책).
        if (!room.isParticipant(memberId)) {
            throw new NotFoundChatRoomException();
        }
        blockPort.validateNotBlocked(memberId, room.otherMemberId(memberId));

        if (imageIds == null) {
            return Optional.empty();
        }
        List<Long> parsedIds = ChatImageIdPolicy.parse(imageIds);
        Map<Long, String> urls = imagePort.requireImageUrls(parsedIds);
        return Optional.of(parsedIds.stream().map(id -> new ChatImage(id, urls.get(id))).toList());
    }
}
