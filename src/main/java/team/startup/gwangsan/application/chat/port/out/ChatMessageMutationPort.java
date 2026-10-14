package team.startup.gwangsan.application.chat.port.out;

import team.startup.gwangsan.application.chat.data.ChatMessageMutationEvent;

public interface ChatMessageMutationPort {
    /** 커밋 후 채팅 서버에 전달된다. rollback 되면 전달하지 않는다. */
    void publish(ChatMessageMutationEvent event);
}
