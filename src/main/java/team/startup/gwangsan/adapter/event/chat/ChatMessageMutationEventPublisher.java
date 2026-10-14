package team.startup.gwangsan.adapter.event.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.chat.data.ChatMessageMutationEvent;
import team.startup.gwangsan.application.chat.port.out.ChatMessageMutationPort;

/** 트랜잭션 안에서 Spring 이벤트로 발행하고, 전달은 ChatMessageMutationEventListener 가 커밋 후에 한다. */
@Component
@RequiredArgsConstructor
public class ChatMessageMutationEventPublisher implements ChatMessageMutationPort {

    private final ApplicationEventPublisher events;

    @Override
    public void publish(ChatMessageMutationEvent event) {
        events.publishEvent(event);
    }
}
