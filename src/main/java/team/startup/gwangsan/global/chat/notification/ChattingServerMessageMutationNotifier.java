package team.startup.gwangsan.global.chat.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import team.startup.gwangsan.global.event.ChatMessageMutationEvent;

@Slf4j
@Component
public class ChattingServerMessageMutationNotifier {
    private final ChattingServerProperties properties;
    private final RestClient restClient;

    public ChattingServerMessageMutationNotifier(ChattingServerProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        if (!properties.isEnabled()) {
            this.restClient = null;
            return;
        }
        RestClient.Builder configured = builder.clone().baseUrl(properties.url());
        if (properties.connectTimeout() != null && properties.readTimeout() != null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(properties.connectTimeout());
            factory.setReadTimeout(properties.readTimeout());
            configured.requestFactory(factory);
        }
        this.restClient = configured.build();
    }

    public void notifyMutation(ChatMessageMutationEvent event) {
        if (restClient == null) {
            log.warn("[CHAT-MUTATION] chatting.server.url is empty; notification skipped. messageId={}", event.messageId());
            return;
        }
        String path = event.kind() == ChatMessageMutationEvent.Kind.UPDATED
                ? "/api/internal/chat/message-updated" : "/api/internal/chat/message-deleted";
        Object body = event.kind() == ChatMessageMutationEvent.Kind.UPDATED
                ? new UpdatedRequest(event.roomId(), event.messageId(), event.content(), event.editedAt(),
                    event.roomListChanged(), event.latestMessage())
                : new DeletedRequest(event.roomId(), event.messageId(), event.roomListChanged(), event.latestMessage());
        restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                .header("x-internal-secret", properties.internalSecret())
                .body(body).retrieve().toBodilessEntity();
    }

    private record UpdatedRequest(Long roomId, Long messageId, String content, java.time.LocalDateTime editedAt,
                                  boolean roomListChanged, ChatMessageMutationEvent.LatestMessage latestMessage) { }

    private record DeletedRequest(Long roomId, Long messageId, boolean roomListChanged,
                                  ChatMessageMutationEvent.LatestMessage latestMessage) { }
}
