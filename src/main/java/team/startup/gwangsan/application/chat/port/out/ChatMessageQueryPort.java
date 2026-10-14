package team.startup.gwangsan.application.chat.port.out;

import team.startup.gwangsan.application.chat.data.ChatMessageRow;
import team.startup.gwangsan.domain.chat.model.ChatImage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ChatMessageQueryPort {

    /** 삭제 제외, (createdAt, id) 내림차순 cursor 페이지. */
    List<ChatMessageRow> findPage(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit);

    Map<Long, List<ChatImage>> findImages(List<Long> messageIds);

    /** lastMessageId 이하의 상대 메시지를 읽음으로 바꾼다. */
    void markRead(Long roomId, Long lastMessageId, Long readerId);
}
