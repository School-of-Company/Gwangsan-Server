package team.startup.gwangsan.adapter.integration.trade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.chat.data.ChatRoomSnapshot;
import team.startup.gwangsan.application.chat.port.in.ChatRoomLookupQuery;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("TradeChatBridge (trade → chat 공개 query)")
class TradeChatBridgeTest {

    private final ChatRoomLookupQuery lookup = mock(ChatRoomLookupQuery.class);
    private final TradeChatBridge bridge = new TradeChatBridge(lookup);

    @Test
    @DisplayName("it_상품·구매자·판매자로 방 ID 를 찾고, 없으면 기존 NotFoundChatRoomException 을 낸다")
    void it_finds_room_by_participants() {
        when(lookup.byProductAndParticipants(20L, 3L, 4L)).thenReturn(Optional.of(new ChatRoomSnapshot(1L, 20L, 3L, 4L)));
        when(lookup.byProductAndParticipants(21L, 3L, 4L)).thenReturn(Optional.empty());

        assertThat(bridge.findRoomId(20L, 3L, 4L)).contains(1L);
        assertThat(bridge.requireRoomId(20L, 3L, 4L)).isEqualTo(1L);
        assertThat(bridge.findRoomId(21L, 3L, 4L)).isEmpty();
        assertThatThrownBy(() -> bridge.requireRoomId(21L, 3L, 4L)).isInstanceOf(NotFoundChatRoomException.class);
    }

    @Test
    @DisplayName("it_대화 여부를 chat 공개 query 에 위임한다")
    void it_delegates_message_check() {
        when(lookup.hasSentMessage(1L, 3L)).thenReturn(true);

        assertThat(bridge.hasMessageFrom(1L, 3L)).isTrue();
        assertThat(bridge.hasMessageFrom(1L, 4L)).isFalse();
    }
}
