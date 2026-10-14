package team.startup.gwangsan.application.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.chat.data.SaveChatMessageCommand;
import team.startup.gwangsan.application.chat.data.SavedChatMessage;
import team.startup.gwangsan.application.chat.port.in.SaveChatMessageUseCase;
import team.startup.gwangsan.application.chat.port.out.ChatBlockPort;
import team.startup.gwangsan.application.chat.port.out.ChatMemberPort;
import team.startup.gwangsan.application.chat.port.out.ChatMessagePort;
import team.startup.gwangsan.application.chat.port.out.ChatPushPort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatMessageException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatSenderException;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.chat.model.MessageType;
import team.startup.gwangsan.domain.chat.model.StoredChatMessage;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SaveChatMessageService implements SaveChatMessageUseCase {

    private final ChatMessagePort messagePort;
    private final ChatRoomPort roomPort;
    private final ChatMemberPort memberPort;
    private final ChatBlockPort blockPort;
    private final ChatPushPort pushPort;

    /**
     * 순서가 계약이다: 기존 메시지면 replay → 발신자/방 부재 → INSERT 선점 → 차단 검사(실패 시 INSERT 포함 rollback)
     * → 이미지 링크 → 푸시. 차단은 INSERT 뒤에 보므로, 이미 커밋된 메시지의 재수신은 차단 이후에도 replay 된다.
     */
    @Override
    @Transactional
    public SavedChatMessage execute(SaveChatMessageCommand command) {
        if (messagePort.exists(command.messageId())) {
            return replay(command);
        }
        if (!memberPort.exists(command.senderId())) {
            throw new NotFoundChatSenderException();
        }
        ChatRoom room = roomPort.load(command.roomId()).orElseThrow(NotFoundChatRoomException::new);
        if (!messagePort.insertIfAbsent(command)) {
            return replay(command);
        }

        Long senderId = command.senderId();
        Long recipientId = room.otherMemberId(senderId);
        blockPort.validateNotBlocked(senderId, recipientId);
        boolean recipientHidden = room.isHiddenFor(recipientId);

        List<ChatImage> images = List.of();
        if (command.messageType() == MessageType.IMAGE && command.imageIds() != null && !command.imageIds().isEmpty()) {
            images = messagePort.linkImages(command.messageId(), command.imageIds());
        }
        if (!recipientHidden && command.messageType() != MessageType.SYSTEM) {
            pushPort.notifyNewMessage(recipientId, command.roomId());
        }
        return new SavedChatMessage(command.messageId(), images, command.createdAt(), senderId, false);
    }

    private SavedChatMessage replay(SaveChatMessageCommand command) {
        StoredChatMessage stored = messagePort.findStored(command.messageId())
                .orElseThrow(NotFoundChatMessageException::new);
        stored.verifySameAs(command.roomId(), command.senderId(), command.content(),
                command.messageType(), command.createdAt());
        if (stored.comparesImages()) {
            Set<Long> requested = command.imageIds() == null
                    ? Set.of()
                    : messagePort.findExistingImageIds(command.imageIds());
            stored.verifySameImages(requested);
        }
        return new SavedChatMessage(stored.messageId(), stored.images(), stored.createdAt(),
                stored.senderId(), stored.checked());
    }
}
