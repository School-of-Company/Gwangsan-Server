package team.startup.gwangsan.domain.chat.model;

import team.startup.gwangsan.domain.chat.exception.ChatMessageIdConflictException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 이미 저장된 메시지. 같은 messageId 가 다시 들어오면 이 값과 비교해 replay 와 충돌을 가른다.
 * content 는 수정 전 원문이다 (수정된 메시지의 replay 도 통과해야 한다).
 */
public record StoredChatMessage(
        Long messageId,
        Long roomId,
        Long senderId,
        String content,
        MessageType messageType,
        LocalDateTime createdAt,
        boolean checked,
        boolean deleted,
        List<ChatImage> images
) {

    /** createdAt 은 stream 의 epoch millis 정밀도까지만 비교한다. */
    public void verifySameAs(Long roomId, Long senderId, String content, MessageType messageType, LocalDateTime createdAt) {
        if (!Objects.equals(this.roomId, roomId)
                || !Objects.equals(this.senderId, senderId)
                || !Objects.equals(this.content, content)
                || this.messageType != messageType
                || !this.createdAt.truncatedTo(ChronoUnit.MILLIS).equals(createdAt.truncatedTo(ChronoUnit.MILLIS))) {
            throw new ChatMessageIdConflictException(messageId);
        }
    }

    /** 삭제된 이미지 메시지는 링크가 지워졌으므로 이미지 집합을 비교하지 않는다. */
    public boolean comparesImages() {
        return messageType == MessageType.IMAGE && !deleted;
    }

    public void verifySameImages(Set<Long> existingRequestedImageIds) {
        Set<Long> storedIds = images.stream().map(ChatImage::imageId).collect(Collectors.toSet());
        if (!storedIds.equals(existingRequestedImageIds)) {
            throw new ChatMessageIdConflictException(messageId);
        }
    }
}
