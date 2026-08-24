package team.startup.gwangsan.global.chat.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

import java.time.LocalDateTime;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ChattingServerTradeStatusNotifierImpl 단위 테스트")
class ChattingServerTradeStatusNotifierImplTest {

    private MockRestServiceServer server;
    private ChattingServerTradeStatusNotifierImpl notifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        notifier = new ChattingServerTradeStatusNotifierImpl(
                new ChattingServerProperties("http://chatting-server", "test-secret"),
                builder
        );
    }

    @Test
    @DisplayName("예약 여부를 포함한 거래 상태를 채팅 서버에 전달한다")
    void it_sends_trade_status_with_reservation_state() {
        server.expect(once(), requestTo("http://chatting-server/api/internal/chat/transaction-state"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-internal-secret", "test-secret"))
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.targetMemberId").value(2))
                .andExpect(jsonPath("$.productId").value(3))
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andExpect(jsonPath("$.isReserved").value(true))
                .andRespond(withSuccess());

        notifier.notifyTradeStatusChanged(new TradeStatusChangedEvent(
                1L,
                2L,
                3L,
                false,
                true,
                LocalDateTime.of(2026, 8, 24, 13, 15)
        ));

        server.verify();
    }
}
