package team.startup.gwangsan.domain.chat.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.chat.entity.ChatMessage;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatMessageException;
import team.startup.gwangsan.domain.chat.presentation.dto.response.UpdateChatMessageResponse;
import team.startup.gwangsan.domain.chat.repository.ChatMessageImageRepository;
import team.startup.gwangsan.domain.chat.repository.ChatMessageRepository;
import team.startup.gwangsan.global.event.ChatMessageMutationEvent;
import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.global.util.MemberUtil;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatMessageMutationService {
    private final ChatMessageRepository messages;
    private final ChatMessageImageRepository images;
    private final MemberUtil memberUtil;
    private final ApplicationEventPublisher events;
    private final EntityManager entityManager;

    @Transactional
    public UpdateChatMessageResponse update(Long messageId, String content) {
        ChatMessage message = ownedRecentMessage(messageId, LocalDateTime.now());
        if (message.getMessageType() != MessageType.TEXT) throw new GlobalException(ErrorCode.CHAT_MESSAGE_NOT_TEXT);
        if (content == null || content.isBlank()) throw new GlobalException(ErrorCode.CHAT_MESSAGE_INVALID_CONTENT);

        boolean latest = isLatest(message);
        LocalDateTime editedAt = LocalDateTime.now();
        message.edit(content, editedAt);
        events.publishEvent(new ChatMessageMutationEvent(ChatMessageMutationEvent.Kind.UPDATED,
                message.getRoom().getId(), messageId, content, editedAt, latest,
                latest ? latestMessage(message) : null));
        return new UpdateChatMessageResponse(messageId, message.getRoom().getId(), content, editedAt);
    }

    @Transactional
    public void delete(Long messageId) {
        ChatMessage message = ownedRecentMessage(messageId, LocalDateTime.now());
        boolean latest = isLatest(message);
        images.deleteByMessageId(messageId);
        message.delete(LocalDateTime.now());
        entityManager.flush();
        ChatMessageMutationEvent.LatestMessage replacement = latest
                ? messages.findFirstByRoomIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(message.getRoom().getId())
                    .map(this::latestMessage).orElse(null)
                : null;
        events.publishEvent(new ChatMessageMutationEvent(ChatMessageMutationEvent.Kind.DELETED,
                message.getRoom().getId(), messageId, null, null, latest, replacement));
    }

    private ChatMessage ownedRecentMessage(Long messageId, LocalDateTime now) {
        ChatMessage message = messages.findByIdForUpdate(messageId)
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(NotFoundChatMessageException::new);
        if (message.getSender() == null || !memberUtil.getCurrentMember().getId().equals(message.getSender().getId())) {
            throw new GlobalException(ErrorCode.CHAT_MESSAGE_FORBIDDEN);
        }
        if (now.isAfter(message.getCreatedAt().plusHours(24))) {
            throw new GlobalException(ErrorCode.CHAT_MESSAGE_EXPIRED);
        }
        return message;
    }

    private boolean isLatest(ChatMessage message) {
        return messages.findFirstByRoomIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(message.getRoom().getId())
                .map(latest -> latest.getId().equals(message.getId())).orElse(false);
    }

    private ChatMessageMutationEvent.LatestMessage latestMessage(ChatMessage message) {
        return new ChatMessageMutationEvent.LatestMessage(message.getId(), message.getContent(),
                message.getMessageType(), message.getCreatedAt(), message.getEditedAt());
    }
}
