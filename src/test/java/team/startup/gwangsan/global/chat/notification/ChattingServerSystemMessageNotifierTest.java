package team.startup.gwangsan.global.chat.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import team.startup.gwangsan.global.event.ReservationCancelledEvent;

import java.time.LocalDateTime;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChattingServerSystemMessageNotifierTest {
    @Test
    void sendsSavedSystemMessageToChatServer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ChattingServerSystemMessageNotifier notifier = new ChattingServerSystemMessageNotifier(
                new ChattingServerProperties("http://chatting-server", "test-secret", null, null), builder);
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 30, 12, 0);
        server.expect(once(), requestTo("http://chatting-server/api/internal/chat/system-message"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-internal-secret", "test-secret"))
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.messageId").value(-2))
                .andExpect(jsonPath("$.senderId").value(3))
                .andExpect(jsonPath("$.content").value("취소자님이 예약을 취소했어요"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andRespond(withSuccess());

        notifier.notifySystemMessage(new ReservationCancelledEvent(
                1L, -2L, 3L, 4L, "취소자님이 예약을 취소했어요", createdAt));

        server.verify();
    }
}
