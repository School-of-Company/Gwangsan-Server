package team.startup.gwangsan.global.event.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.global.chat.notification.ChattingServerTradeStatusNotifier;
import team.startup.gwangsan.global.event.TradeStatusChangedEvent;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("TradeStatusChangedEventListener 단위 테스트")
class TradeStatusChangedEventListenerTest {

    @Test
    @DisplayName("채팅 서버 알림 실패가 발생해도 예외를 전파하지 않는다")
    void it_does_not_propagate_exception_when_chatting_server_notification_fails() {
        ChattingServerTradeStatusNotifier notifier = mock(ChattingServerTradeStatusNotifier.class);
        TradeStatusChangedEventListener listener = new TradeStatusChangedEventListener(notifier);
        TradeStatusChangedEvent event = new TradeStatusChangedEvent(1L, 2L, 10L, false, LocalDateTime.now());

        doThrow(new RuntimeException("chatting server unavailable"))
                .when(notifier)
                .notifyTradeStatusChanged(event);

        assertDoesNotThrow(() -> listener.handleTradeStatusChangedEvent(event));

        verify(notifier).notifyTradeStatusChanged(event);
    }
}
