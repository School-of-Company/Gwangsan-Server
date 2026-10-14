package team.startup.gwangsan.application.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.chat.port.out.ChatBlockPort;
import team.startup.gwangsan.application.chat.port.out.ChatImagePort;
import team.startup.gwangsan.application.chat.port.out.ChatRoomPort;
import team.startup.gwangsan.domain.chat.exception.InvalidChatImageIdsException;
import team.startup.gwangsan.domain.chat.exception.NotFoundChatRoomException;
import team.startup.gwangsan.domain.chat.model.ChatImage;
import team.startup.gwangsan.domain.chat.model.ChatRoom;
import team.startup.gwangsan.domain.common.ActorContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateChatSendableService 단위 테스트")
class ValidateChatSendableServiceTest {

    private static final ActorContext ACTOR = new ActorContext(1L, ActorContext.Role.ROLE_USER);

    @Mock private ChatRoomPort roomPort;
    @Mock private ChatBlockPort blockPort;
    @Mock private ChatImagePort imagePort;
    @InjectMocks private ValidateChatSendableService service;

    private void givenParticipantRoom() {
        when(roomPort.load(10L)).thenReturn(Optional.of(new ChatRoom(10L, 20L, 1L, 2L, null, null)));
    }

    @Nested
    @DisplayName("방·차단 검사는")
    class Describe_room {

        @Test
        @DisplayName("it_방이 없거나 비참여자면 404 이고 차단·이미지를 보지 않는다")
        void it_hides_room_from_strangers() {
            when(roomPort.load(10L)).thenReturn(Optional.empty());
            when(roomPort.load(11L)).thenReturn(Optional.of(new ChatRoom(11L, 20L, 7L, 8L, null, null)));

            assertThatThrownBy(() -> service.query(ACTOR, 10L, List.of("1"))).isInstanceOf(NotFoundChatRoomException.class);
            assertThatThrownBy(() -> service.query(ACTOR, 11L, List.of("1"))).isInstanceOf(NotFoundChatRoomException.class);
            verifyNoInteractions(blockPort, imagePort);
        }

        @Test
        @DisplayName("it_상대와 차단 관계면 이미지 검증 전에 실패한다")
        void it_checks_block_with_other_member() {
            givenParticipantRoom();
            RuntimeException blocked = new RuntimeException("blocked");
            doThrow(blocked).when(blockPort).validateNotBlocked(1L, 2L);

            assertThatThrownBy(() -> service.query(ACTOR, 10L, List.of("bad"))).isSameAs(blocked);
            verifyNoInteractions(imagePort);
        }

        @Test
        @DisplayName("it_imageIds 가 없으면 빈 결과로 통과한다")
        void it_passes_without_images() {
            givenParticipantRoom();

            assertThat(service.query(ACTOR, 10L, null)).isEmpty();
            verifyNoInteractions(imagePort);
        }
    }

    @Nested
    @DisplayName("이미지 검증은")
    class Describe_images {

        @Test
        @DisplayName("it_형식 오류는 조회 없이 400 이다")
        void it_rejects_invalid_ids_without_lookup() {
            givenParticipantRoom();

            assertThatThrownBy(() -> service.query(ACTOR, 10L, List.of())).isInstanceOf(InvalidChatImageIdsException.class);
            assertThatThrownBy(() -> service.query(ACTOR, 10L, List.of("1", "x"))).isInstanceOf(InvalidChatImageIdsException.class);
            verifyNoInteractions(imagePort);
        }

        @Test
        @DisplayName("it_요청 순서와 중복을 보존한다")
        void it_keeps_requested_order_and_duplicates() {
            givenParticipantRoom();
            when(imagePort.requireImageUrls(List.of(12L, 11L, 12L))).thenReturn(Map.of(11L, "u11", 12L, "u12"));

            assertThat(service.query(ACTOR, 10L, List.of("12", "11", "12"))).contains(List.of(
                    new ChatImage(12L, "u12"), new ChatImage(11L, "u11"), new ChatImage(12L, "u12")));
        }

        @Test
        @DisplayName("it_이미지 부재 오류는 그대로 전파한다")
        void it_propagates_missing_image() {
            givenParticipantRoom();
            RuntimeException missing = new RuntimeException("missing");
            when(imagePort.requireImageUrls(List.of(11L))).thenThrow(missing);

            assertThatThrownBy(() -> service.query(ACTOR, 10L, List.of("11"))).isSameAs(missing);
        }
    }
}
