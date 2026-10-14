package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.chat.model.MessageType;

import java.time.LocalDateTime;

/**
 * 메시지 수정/삭제가 커밋된 뒤 채팅 서버에 알릴 내용. roomListChanged 이면 latestMessage 가 방 목록의 새 최신 메시지다.
 * 필드 이름은 채팅 서버 내부 API body 와 같다.
 */
public record ChatMessageMutationEvent(
        Kind kind, Long roomId, Long messageId, String content, LocalDateTime editedAt,
        boolean roomListChanged, LatestMessage latestMessage
) {
    public enum Kind { UPDATED, DELETED }

    public record LatestMessage(Long messageId, String content, MessageType messageType,
                                LocalDateTime createdAt, LocalDateTime editedAt) {
    }
}
