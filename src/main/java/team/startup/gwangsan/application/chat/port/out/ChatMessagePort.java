package team.startup.gwangsan.application.chat.port.out;

import team.startup.gwangsan.application.chat.data.SaveChatMessageCommand;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.ChatMessage;
import team.startup.gwangsan.domain.chat.model.StoredChatMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChatMessagePort {

    boolean exists(Long messageId);

    /**
     * 같은 ID 가 이미 커밋돼 있으면 false. 중복 키 실패가 현재 트랜잭션을 rollback-only 로 만들지 않아야 한다.
     */
    boolean insertIfAbsent(SaveChatMessageCommand message);

    /** 공유 잠금으로 읽어 다른 트랜잭션이 막 커밋한 값과 이미지 링크를 본다. */
    Optional<StoredChatMessage> findStored(Long messageId);

    /** 요청 이미지 중 실제 존재하는 ID (공유 잠금). */
    Set<Long> findExistingImageIds(List<Long> imageIds);

    /** 존재하는 이미지만 링크한다 (없는 ID 는 무시, 중복 ID 는 한 번). */
    List<ChatImage> linkImages(Long messageId, List<Long> imageIds);

    /** 쓰기 잠금. */
    Optional<ChatMessage> findForUpdate(Long messageId);

    /** 삭제되지 않은 최신 메시지 (SYSTEM 포함). */
    Optional<ChatMessage> findLatest(Long roomId);

    void saveEdit(ChatMessage message);

    /** 이미지 링크 삭제 + deletedAt 기록을 즉시 반영한다 (이후 최신 메시지 재조회가 보도록). */
    void saveDeletion(ChatMessage message);

    boolean hasSentMessage(Long roomId, Long senderId);

    /** 읽음 처리된 SYSTEM 메시지를 지정한 ID·시각으로 저장한다. */
    Long saveSystemMessage(Long roomId, Long messageId, Long senderId, String content, LocalDateTime createdAt);
}
