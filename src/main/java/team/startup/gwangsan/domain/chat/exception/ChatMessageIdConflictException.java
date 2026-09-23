package team.startup.gwangsan.domain.chat.exception;

public class ChatMessageIdConflictException extends RuntimeException {
    public ChatMessageIdConflictException(Long messageId) {
        super("Conflicting payload for chat message " + messageId);
    }
}
