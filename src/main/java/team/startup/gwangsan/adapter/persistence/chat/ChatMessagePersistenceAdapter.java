package team.startup.gwangsan.adapter.persistence.chat;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.application.chat.data.ChatMessageRow;
import team.startup.gwangsan.application.chat.data.SaveChatMessageCommand;
import team.startup.gwangsan.application.chat.port.out.ChatMessagePort;
import team.startup.gwangsan.application.chat.port.out.ChatMessageQueryPort;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.ChatMessage;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.chat.model.StoredChatMessage;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatMessagePersistenceAdapter implements ChatMessagePort, ChatMessageQueryPort {

    private final ChatMessageJpaRepository messages;
    private final ChatMessageImageJpaRepository imageLinks;
    private final ChatRoomJpaRepository rooms;
    private final EntityManager entityManager;

    @Override
    public boolean exists(Long messageId) {
        return messages.existsById(messageId);
    }

    @Override
    public boolean insertIfAbsent(SaveChatMessageCommand message) {
        return messages.insertIfAbsent(message);
    }

    @Override
    public Optional<StoredChatMessage> findStored(Long messageId) {
        return messages.findStoredMessage(messageId);
    }

    @Override
    public Set<Long> findExistingImageIds(List<Long> imageIds) {
        return messages.findExistingImageIds(imageIds);
    }

    @Override
    public List<ChatImage> linkImages(Long messageId, List<Long> imageIds) {
        // ID 목록 IN 조회라 없는 ID 는 빠지고 중복 ID 는 한 번만 나온다 (기존 findAllById 와 같은 의미).
        List<ImageJpaEntity> images = entityManager.createQuery("SELECT i FROM Image i WHERE i.id IN :ids", ImageJpaEntity.class)
                .setParameter("ids", imageIds)
                .getResultList();
        ChatMessageJpaEntity message = messages.getReferenceById(messageId);
        imageLinks.saveAll(images.stream()
                .map(image -> ChatMessageImageJpaEntity.builder().image(image).chatMessage(message).build())
                .toList());
        return images.stream().map(image -> new ChatImage(image.getId(), image.getImageUrl())).toList();
    }

    @Override
    public Optional<ChatMessage> findForUpdate(Long messageId) {
        return messages.findByIdForUpdate(messageId).map(ChatMessagePersistenceAdapter::toDomain);
    }

    @Override
    public Optional<ChatMessage> findLatest(Long roomId) {
        return messages.findFirstByRoomIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(roomId)
                .map(ChatMessagePersistenceAdapter::toDomain);
    }

    @Override
    public void saveEdit(ChatMessage message) {
        apply(message);
    }

    @Override
    public void saveDeletion(ChatMessage message) {
        imageLinks.deleteByMessageId(message.id());
        apply(message);
        entityManager.flush();
    }

    @Override
    public boolean hasSentMessage(Long roomId, Long senderId) {
        return messages.existsByRoomAndSenderId(rooms.getReferenceById(roomId), senderId);
    }

    @Override
    public Long saveSystemMessage(Long roomId, Long messageId, Long senderId, String content, LocalDateTime createdAt) {
        return messages.save(ChatMessageJpaEntity.builder()
                .id(messageId)
                .room(rooms.getReferenceById(roomId))
                .sender(entityManager.getReference(MemberJpaEntity.class, senderId))
                .content(content)
                .messageType(MessageType.SYSTEM)
                .checked(true)
                .createdAt(createdAt)
                .build()).getId();
    }

    @Override
    public List<ChatMessageRow> findPage(Long roomId, LocalDateTime lastCreatedAt, Long lastMessageId, int limit) {
        return messages.findChatMessageByRoomIdWithCursorPaging(roomId, lastCreatedAt, lastMessageId, limit).stream()
                .map(message -> new ChatMessageRow(
                        message.getId(),
                        message.getRoom().getId(),
                        message.getContent(),
                        message.getMessageType(),
                        message.getCreatedAt(),
                        message.getEditedAt(),
                        message.getSender().getId(),
                        message.getSender().getNickname(),
                        message.getChecked()))
                .toList();
    }

    @Override
    public Map<Long, List<ChatImage>> findImages(List<Long> messageIds) {
        return imageLinks.findAllByChatMessageIdIn(messageIds).stream()
                .collect(Collectors.groupingBy(
                        link -> link.getChatMessage().getId(),
                        Collectors.mapping(
                                link -> new ChatImage(link.getImage().getId(), link.getImage().getImageUrl()),
                                Collectors.toList())));
    }

    @Override
    public void markRead(Long roomId, Long lastMessageId, Long readerId) {
        messages.readMessage(roomId, lastMessageId, readerId);
    }

    // 잠금으로 읽어 둔 managed Entity 에 도메인 결과를 명시적으로 기록한다.
    private void apply(ChatMessage message) {
        messages.getReferenceById(message.id()).applyMutation(
                message.content(), message.originalContent(), message.editedAt(), message.deletedAt());
    }

    private static ChatMessage toDomain(ChatMessageJpaEntity message) {
        return new ChatMessage(
                message.getId(),
                message.getRoom().getId(),
                message.getSender() == null ? null : message.getSender().getId(),
                message.getContent(),
                message.getOriginalContent(),
                message.getMessageType(),
                message.getCreatedAt(),
                message.getEditedAt(),
                message.getDeletedAt());
    }
}
