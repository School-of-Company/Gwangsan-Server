package team.startup.gwangsan.domain.chat.service;

public interface ReadChatMessageService {
    void execute(Long roomId, Long lastMessage);
}
