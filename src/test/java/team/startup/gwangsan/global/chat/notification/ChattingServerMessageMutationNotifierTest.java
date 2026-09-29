package team.startup.gwangsan.global.chat.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import team.startup.gwangsan.domain.chat.entity.constant.MessageType;
import team.startup.gwangsan.global.event.ChatMessageMutationEvent;

import java.time.LocalDateTime;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChattingServerMessageMutationNotifierTest {
    private MockRestServiceServer server;
    private ChattingServerMessageMutationNotifier notifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        notifier = new ChattingServerMessageMutationNotifier(
                new ChattingServerProperties("http://chatting-server", "test-secret", null, null), builder);
    }

    @Test
    void sends_updated_message_with_new_room_head() {
        server.expect(once(), requestTo("http://chatting-server/api/internal/chat/message-updated"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-internal-secret", "test-secret"))
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.messageId").value(2))
                .andExpect(jsonPath("$.content").value("수정"))
                .andExpect(jsonPath("$.editedAt").exists())
                .andExpect(jsonPath("$.roomListChanged").value(true))
                .andExpect(jsonPath("$.latestMessage.messageId").value(2))
                .andRespond(withSuccess());
        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 12, 0);
        notifier.notifyMutation(new ChatMessageMutationEvent(ChatMessageMutationEvent.Kind.UPDATED,
                1L, 2L, "수정", now, true,
                new ChatMessageMutationEvent.LatestMessage(2L, "수정", MessageType.TEXT, now.minusMinutes(1), now)));
        server.verify();
    }

    @Test
    void sends_deleted_message_with_empty_room_head() {
        server.expect(once(), requestTo("http://chatting-server/api/internal/chat/message-deleted"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.messageId").value(2))
                .andExpect(jsonPath("$.roomListChanged").value(true))
                .andExpect(jsonPath("$.latestMessage").value(org.hamcrest.Matchers.nullValue()))
                .andRespond(withSuccess());
        notifier.notifyMutation(new ChatMessageMutationEvent(ChatMessageMutationEvent.Kind.DELETED,
                1L, 2L, null, null, true, null));
        server.verify();
    }
}
