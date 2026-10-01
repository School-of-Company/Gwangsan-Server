package team.startup.gwangsan.global.chat.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import team.startup.gwangsan.global.event.ReservationCancelledEvent;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ChattingServerSystemMessageNotifier {
    private final ChattingServerProperties properties;
    private final RestClient restClient;

    public ChattingServerSystemMessageNotifier(ChattingServerProperties properties, RestClient.Builder builder) {
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

    public void notifySystemMessage(ReservationCancelledEvent event) {
        if (restClient == null) {
            log.warn("chatting.server.url is empty; system message delivery skipped. roomId={}", event.roomId());
            return;
        }
        restClient.post().uri("/api/internal/chat/system-message")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-internal-secret", properties.internalSecret())
                .body(new Request(event.roomId(), event.messageId(), event.senderId(), event.content(), event.createdAt()))
                .retrieve().toBodilessEntity();
    }

    private record Request(Long roomId, Long messageId, Long senderId, String content, LocalDateTime createdAt) {
    }
}
