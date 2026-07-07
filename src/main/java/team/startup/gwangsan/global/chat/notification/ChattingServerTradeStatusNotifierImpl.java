package team.startup.gwangsan.global.chat.notification;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

import java.time.LocalDateTime;

@Component
public class ChattingServerTradeStatusNotifierImpl implements ChattingServerTradeStatusNotifier {

    private static final String INTERNAL_SECRET_HEADER = "x-internal-secret";
    private static final String TRADE_STATUS_PATH = "/api/internal/chat/transaction-state";

    private final ChattingServerProperties properties;
    private final RestClient restClient;

    public ChattingServerTradeStatusNotifierImpl(ChattingServerProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = properties.isEnabled() ? restClientBuilder.baseUrl(properties.url()).build() : null;
    }

    @Override
    public void notifyTradeStatusChanged(TradeStatusChangedEvent event) {
        if (restClient == null) {
            return;
        }

        restClient.post()
                .uri(TRADE_STATUS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(INTERNAL_SECRET_HEADER, properties.internalSecret())
                .body(new TradeStatusChangedRequest(
                        event.roomId(),
                        event.targetMemberId(),
                        event.productId(),
                        event.completed(),
                        event.changedAt()
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private record TradeStatusChangedRequest(
            Long roomId,
            Long targetMemberId,
            Long productId,
            boolean isCompleted,
            LocalDateTime createdAt
    ) {
    }
}
