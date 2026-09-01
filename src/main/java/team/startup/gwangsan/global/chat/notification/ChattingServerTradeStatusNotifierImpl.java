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
            log.error("[TRADE-NOTIFY] DISABLED at startup: chatting.server.url 이 비어 있어 거래 상태 변경이 채팅 서버에 전달되지 않습니다.");
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
        log.info("[TRADE-NOTIFY] enabled. baseUrl={}, secretConfigured={}, connectTimeout={}, readTimeout={}",
                properties.url(),
                properties.internalSecret() != null && !properties.internalSecret().isBlank(),
                properties.connectTimeout(),
                properties.readTimeout());
    }

    @Override
    public void notifyTradeStatusChanged(TradeStatusChangedEvent event) {
        if (restClient == null) {
            log.error("[TRADE-NOTIFY] SKIPPED: chatting.server.url 이 비어 있음. roomId={}, productId={}",
                    event.roomId(), event.productId());
            return;
        }

        log.info("[TRADE-NOTIFY] POST {}{} roomId={}, productId={}, completed={}, reserved={}",
                properties.url(), TRADE_STATUS_PATH, event.roomId(), event.productId(), event.completed(), event.reserved());

        var response = restClient.post()
                .uri(TRADE_STATUS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(INTERNAL_SECRET_HEADER, properties.internalSecret())
                .body(new TradeStatusChangedRequest(
                        event.roomId(),
                        event.productId(),
                        event.completed(),
                        event.reserved(),
                        event.requestedBySeller(),
                        event.requestedAt()
                ))
                .retrieve()
                .toBodilessEntity();

        log.info("[TRADE-NOTIFY] chatting server responded {}. roomId={}, productId={}",
                response.getStatusCode(), event.roomId(), event.productId());
    }

    /**
     * 채팅 서버 내부 API 의 요청 스키마.
     *
     * <p>필드명은 클라이언트가 읽는 이름을 그대로 따른다. createdAt 은 상품 생성 시각이
     * 아니라 거래 요청 생성 시각이며, 채팅방 조회 응답의 같은 이름 필드와 값이 일치한다.
     */
    private record TradeStatusChangedRequest(
            Long roomId,
            Long productId,
            boolean isCompleted,
            boolean isReserved,
            Boolean requestedBySeller,
            LocalDateTime createdAt
    ) {
    }
}
