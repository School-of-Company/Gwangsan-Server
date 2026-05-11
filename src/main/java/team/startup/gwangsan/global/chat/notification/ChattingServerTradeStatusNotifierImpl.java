package team.startup.gwangsan.global.chat.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ChattingServerTradeStatusNotifierImpl implements ChattingServerTradeStatusNotifier {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String TRADE_STATUS_PATH = "/internal/trade-status";

    private final ChattingServerProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public void notifyTradeStatusChanged(TradeStatusChangedEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        restClientBuilder
                .baseUrl(properties.url())
                .build()
                .post()
                .uri(TRADE_STATUS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(INTERNAL_SECRET_HEADER, properties.internalSecret())
                .body(new TradeStatusChangedRequest(
                        event.roomId(),
                        event.productId(),
                        event.completed(),
                        event.changedAt()
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private record TradeStatusChangedRequest(
            Long roomId,
            Long productId,
            boolean isCompleted,
            LocalDateTime createdAt
    ) {
    }
}
