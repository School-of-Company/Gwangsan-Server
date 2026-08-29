package team.startup.gwangsan.global.chat.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ChattingServerTradeStatusNotifierImpl implements ChattingServerTradeStatusNotifier {

    private static final String INTERNAL_SECRET_HEADER = "x-internal-secret";
    private static final String TRADE_STATUS_PATH = "/api/internal/chat/transaction-state";

    private final ChattingServerProperties properties;
    private final RestClient restClient;

    public ChattingServerTradeStatusNotifierImpl(ChattingServerProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;

        if (!properties.isEnabled()) {
            // 설정이 비어 있으면 거래 상태가 채팅 서버에 전혀 전달되지 않는다.
            // 조용히 넘어가면 실시간 갱신이 죽은 것을 알 수 없으므로 남겨 둔다.
            log.warn("chatting.server.url 이 설정되지 않아 거래 상태 변경을 채팅 서버에 전달하지 않습니다.");
            this.restClient = null;
            return;
        }

        RestClient.Builder builder = restClientBuilder.clone().baseUrl(properties.url());

        if (properties.connectTimeout() != null && properties.readTimeout() != null) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.connectTimeout());
            requestFactory.setReadTimeout(properties.readTimeout());
            builder.requestFactory(requestFactory);
        }

        this.restClient = builder.build();
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
                        event.reserved(),
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
            boolean isReserved,
            LocalDateTime createdAt
    ) {
    }
}
