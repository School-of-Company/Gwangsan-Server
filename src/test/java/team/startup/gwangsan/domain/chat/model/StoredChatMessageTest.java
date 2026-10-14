package team.startup.gwangsan.domain.chat.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import team.startup.gwangsan.domain.chat.exception.ChatMessageIdConflictException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StoredChatMessage replay 비교")
class StoredChatMessageTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 1, 12, 0, 0, 123_000_000);

    private static StoredChatMessage stored(MessageType type, boolean deleted, List<ChatImage> images) {
        return new StoredChatMessage(9L, 1L, 2L, "hello", type, CREATED, false, deleted, images);
    }

    @Nested
    @DisplayName("verifySameAs() 는")
    class Describe_verifySameAs {

        @ParameterizedTest
        @ValueSource(strings = {"room", "sender", "content", "type", "millis"})
        @DisplayName("it_방·발신자·내용·유형·밀리초 중 하나라도 다르면 충돌이다")
        void it_rejects_any_difference(String field) {
            StoredChatMessage message = stored(MessageType.TEXT, false, List.of());
            assertThatThrownBy(() -> message.verifySameAs(
                    field.equals("room") ? 99L : 1L,
                    field.equals("sender") ? 99L : 2L,
                    field.equals("content") ? "other" : "hello",
                    field.equals("type") ? MessageType.IMAGE : MessageType.TEXT,
                    field.equals("millis") ? CREATED.plusNanos(1_000_000) : CREATED))
                    .isInstanceOf(ChatMessageIdConflictException.class);
        }

        @Test
        @DisplayName("it_밀리초 미만 차이는 같은 메시지로 본다")
        void it_ignores_sub_millisecond_difference() {
            assertThatCode(() -> stored(MessageType.TEXT, false, List.of())
                    .verifySameAs(1L, 2L, "hello", MessageType.TEXT, CREATED.plusNanos(999_999)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("이미지 비교는")
    class Describe_images {

        @Test
        @DisplayName("it_삭제되지 않은 IMAGE 만 비교 대상이다")
        void it_compares_only_live_image_messages() {
            assertThat(stored(MessageType.IMAGE, false, List.of()).comparesImages()).isTrue();
            assertThat(stored(MessageType.IMAGE, true, List.of()).comparesImages()).isFalse();
            assertThat(stored(MessageType.TEXT, false, List.of()).comparesImages()).isFalse();
        }

        @Test
        @DisplayName("it_이미지 ID 집합이 같으면 통과하고 다르면 충돌이다")
        void it_compares_image_id_sets() {
            StoredChatMessage message = stored(MessageType.IMAGE, false,
                    List.of(new ChatImage(3L, "a"), new ChatImage(1L, "b")));
            assertThatCode(() -> message.verifySameImages(Set.of(1L, 3L))).doesNotThrowAnyException();
            assertThatThrownBy(() -> message.verifySameImages(Set.of(1L)))
                    .isInstanceOf(ChatMessageIdConflictException.class);
            assertThatCode(() -> stored(MessageType.IMAGE, false, List.of()).verifySameImages(Set.of()))
                    .doesNotThrowAnyException();
        }
    }
}
