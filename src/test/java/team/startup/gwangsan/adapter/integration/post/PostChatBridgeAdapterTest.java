package team.startup.gwangsan.adapter.integration.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.chat.data.ChatRoomSnapshot;
import team.startup.gwangsan.application.chat.port.in.ChatRoomLookupQuery;
import team.startup.gwangsan.application.chat.port.in.SaveChatSystemMessageUseCase;
import team.startup.gwangsan.application.post.data.ChatRoomView;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("PostChatBridgeAdapter (post → chat 공개 Port)")
class PostChatBridgeAdapterTest {

    private final ChatRoomLookupQuery lookup = mock(ChatRoomLookupQuery.class);
    private final SaveChatSystemMessageUseCase systemMessages = mock(SaveChatSystemMessageUseCase.class);
    private final PostChatBridgeAdapter adapter = new PostChatBridgeAdapter(lookup, systemMessages);

    @Nested
    @DisplayName("방 조회는")
    class Describe_find_room {

        @Test
        @DisplayName("it_방 ID·상품+회원 조회 결과를 같은 필드의 ChatRoomView 로 옮긴다")
        void it_maps_snapshot_to_view() {
            when(lookup.byRoomId(1L)).thenReturn(Optional.of(new ChatRoomSnapshot(1L, 20L, 3L, 4L)));
            when(lookup.byProductAndMember(20L, 3L)).thenReturn(Optional.of(new ChatRoomSnapshot(1L, 20L, 3L, 4L)));
            when(lookup.byRoomId(2L)).thenReturn(Optional.empty());

            assertThat(adapter.findRoom(1L)).contains(new ChatRoomView(1L, 20L, 3L, 4L));
            assertThat(adapter.findRoom(20L, 3L)).contains(new ChatRoomView(1L, 20L, 3L, 4L));
            assertThat(adapter.findRoom(2L)).isEmpty();
        }
    }

    @Test
    @DisplayName("it_대화 여부와 시스템 메시지 저장을 chat 공개 Port 에 그대로 위임한다")
    void it_delegates_message_operations() {
        LocalDateTime at = LocalDateTime.of(2026, 10, 14, 9, 0);
        when(lookup.hasSentMessage(1L, 3L)).thenReturn(true);
        when(systemMessages.save(1L, 77L, 4L, "예약이 취소되었습니다.", at)).thenReturn(77L);

        assertThat(adapter.hasSentMessage(1L, 3L)).isTrue();
        assertThat(adapter.saveSystemMessage(1L, 77L, 4L, "예약이 취소되었습니다.", at)).isEqualTo(77L);
    }
}
