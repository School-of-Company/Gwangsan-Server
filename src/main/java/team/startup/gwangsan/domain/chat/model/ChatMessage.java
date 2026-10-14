package team.startup.gwangsan.domain.chat.model;

import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.domain.common.exception.GlobalException;

import java.time.LocalDateTime;

/**
 * 수정·삭제 대상 메시지. originalContent 는 첫 수정 직전 내용이며 stream replay 비교 기준이다.
 */
public record ChatMessage(
        Long id,
        Long roomId,
        Long senderId,
        String content,
        String originalContent,
        MessageType messageType,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        LocalDateTime deletedAt
) {

    private static final long MUTABLE_HOURS = 24;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 본인(403) → 24시간(409) 순서로 검사한다. */
    public void assertMutableBy(Long actorId, LocalDateTime now) {
        if (senderId == null || !senderId.equals(actorId)) {
            throw new GlobalException(ErrorCode.CHAT_MESSAGE_FORBIDDEN);
        }
        if (now.isAfter(createdAt.plusHours(MUTABLE_HOURS))) {
            throw new GlobalException(ErrorCode.CHAT_MESSAGE_EXPIRED);
        }
    }

    public void assertEditable(String newContent) {
        if (messageType != MessageType.TEXT) {
            throw new GlobalException(ErrorCode.CHAT_MESSAGE_NOT_TEXT);
        }
        if (newContent == null || newContent.isBlank()) {
            throw new GlobalException(ErrorCode.CHAT_MESSAGE_INVALID_CONTENT);
        }
    }

    public ChatMessage edit(String newContent, LocalDateTime editedAt) {
        assertEditable(newContent);
        return new ChatMessage(id, roomId, senderId, newContent,
                originalContent == null ? content : originalContent,
                messageType, createdAt, editedAt, deletedAt);
    }

    public ChatMessage delete(LocalDateTime deletedAt) {
        return new ChatMessage(id, roomId, senderId, content, originalContent,
                messageType, createdAt, editedAt, deletedAt);
    }
}
