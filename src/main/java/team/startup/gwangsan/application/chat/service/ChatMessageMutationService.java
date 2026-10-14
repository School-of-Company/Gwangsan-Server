package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.data.ChatMessageMutationEvent;
import team.startup.gwangsan.application.chat.data.UpdatedChatMessage;
import team.startup.gwangsan.application.chat.port.in.DeleteChatMessageUseCase;
import team.startup.gwangsan.application.chat.port.in.UpdateChatMessageUseCase;
import team.startup.gwangsan.application.chat.port.out.ChatMessageMutationPort;
import team.startup.gwangsan.application.chat.port.out.ChatMessagePort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatMessageException;
import team.startup.gwangsan.domain.chat.model.ChatMessage;
import team.startup.gwangsan.domain.common.ActorContext;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatMessageMutationService implements UpdateChatMessageUseCase, DeleteChatMessageUseCase {

    private final ChatMessagePort messagePort;
    private final ChatMessageMutationPort mutationPort;

    @Override
    @Transactional
    public UpdatedChatMessage execute(ActorContext actor, Long messageId, String content) {
        ChatMessage message = ownedRecentMessage(actor, messageId);
        message.assertEditable(content);
        boolean latest = isLatest(message);
        ChatMessage edited = message.edit(content, LocalDateTime.now());
        messagePort.saveEdit(edited);
        mutationPort.publish(new ChatMessageMutationEvent(ChatMessageMutationEvent.Kind.UPDATED,
                edited.roomId(), messageId, content, edited.editedAt(), latest,
                latest ? latestMessage(edited) : null));
        return new UpdatedChatMessage(messageId, edited.roomId(), content, edited.editedAt());
    }

    @Override
    @Transactional
    public void execute(ActorContext actor, Long messageId) {
        ChatMessage message = ownedRecentMessage(actor, messageId);
        boolean latest = isLatest(message);
        messagePort.saveDeletion(message.delete(LocalDateTime.now()));
        ChatMessageMutationEvent.LatestMessage replacement = latest
                ? messagePort.findLatest(message.roomId()).map(this::latestMessage).orElse(null)
                : null;
        mutationPort.publish(new ChatMessageMutationEvent(ChatMessageMutationEvent.Kind.DELETED,
                message.roomId(), messageId, null, null, latest, replacement));
    }

    private ChatMessage ownedRecentMessage(ActorContext actor, Long messageId) {
        ChatMessage message = messagePort.findForUpdate(messageId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(NotFoundChatMessageException::new);
        message.assertMutableBy(actor.memberId(), LocalDateTime.now());
        return message;
    }

    private boolean isLatest(ChatMessage message) {
        return messagePort.findLatest(message.roomId())
                .map(latest -> latest.id().equals(message.id()))
                .orElse(false);
    }

    private ChatMessageMutationEvent.LatestMessage latestMessage(ChatMessage message) {
        return new ChatMessageMutationEvent.LatestMessage(message.id(), message.content(),
                message.messageType(), message.createdAt(), message.editedAt());
    }
}
